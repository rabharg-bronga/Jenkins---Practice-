job('github-suspend-user') {

authorization {
    permission('hudson.model.Item.Build:authenticated')
    permission('hudson.model.Item.Cancel:authenticated')
}

wrappers {
    credentialsBinding {
        string('ONELOGON_BASE64_TOKEN', 'onelogon-base64-token')
        usernamePassword('GITHUB_USERNAME', 'GITHUB_TOKEN', 'github_credential')
    }
}


steps {
    shell('''\
        #!/bin/bash

        PARENT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
        
        dormant_file='dormant_users.csv'
        suspended_file='suspended_users.csv'
        
        # Download Dormant users from GitHub

        curl -u $GITHUB_USERNAME:$GITHUB_TOKEN -o $PARENT_PATH/$dormant_file  https://github.gapinc.com/stafftools/reports/$dormant_file
        
        FILE="${PARENT_PATH}/${dormant_file}"
        
        # Checking whether the file exists or not

        if [[ -f "$FILE" ]]; then
           awk 'BEGIN{FS=OFS=","} NF!=14{print "not enough fields"; exit}' $FILE
            echo "file:$FILE exists and has valid content."
        else 
           echo "file:$FILE does not exists or not a valid content."
            echo "Retrying once.."
            curl -u $GITHUB_USERNAME:$GITHUB_TOKEN -o $PARENT_PATH/$dormant_file  https://github.gapinc.com/stafftools/reports/$dormant_file
           awk 'BEGIN{FS=OFS=","} NF!=14{print "not enough fields"; exit}' $FILE
            echo "file:$FILE exists and has valid content."
        fi
        
        # Fetching access token

        access_token=$(curl -H "Authorization: Basic ${ONELOGON_BASE64}" -H "Content-Type: application/x-www-form-urlencoded" -X POST "https://onelogon.gap.com/as/token.oauth2?grant_type=client_credentials" | jq -r .access_token)
        
        echo "access_token $access_token"
        
        AD_Group="test-BRANCH"
        
        echo "User","Suspended Status" >> ${PARENT_PATH}/${suspended_file}
        
        # Converting First letter of ADID to Upper case

        tail -n +2 /home/jenkins/$dormant_file | cut -d"," -f 3 | awk '{$1=toupper(substr($1,0,1))substr($1,2)}1' > dormant_users_list.txt
        
        # Storing the list of Leaders in an array


        final_leaders_list=$(cat /home/jenkins/exclude_dormant_leaderss_test.txt) && echo $final_leaders_list
        
        
        
        while read line
        do
            user=$(echo $line)
            echo "User ${user}"
            if [[ ! ${final_leaders_list} =~ "${user}" ]]; then
                echo "inside first if"
                if [[ "${user}" == "S-"* ]]; then
                    echo "dormant users having service accounts..not performing any action since it is a service account(s)"
                else
                    command=$(curl -H "Authorization: Bearer ${access_token}" -H "Content-Type: application/json" -H "Accept: application/json" -X POST -d '{"userId":"'${user}'","platform":"Active_Directory","groups":["'${AD_Group}'"]}' "https://myidentity.gapinc.com/identityiq/ids/groups/removeUserFromGroups" | jq -r .status)
                    if [[ ${command} == "success" ]]; then
                        echo "user:${user} removed successfully from AD Group:${AD_Group}"
                        echo "${user}","user:${user} removed successfully from AD Group:${AD_Group}" >> ${PARENT_PATH}/${suspended_file}
                    else 
                        echo "Unable to remove user:${user} from AD Group:${AD_Group}"
                        echo "${user}","Unable to remove user:${user} from AD Group:${AD_Group}" >> ${PARENT_PATH}/${suspended_file}
                    fi
                fi
            fi
        done < dormant_users_list.txt
        
        
        cp ${PARENT_PATH}/${suspended_file} ${WORKSPACE}
        cd /home/jenkins/suspended_users
        rm -rf ${suspended_file}
        cp ${PARENT_PATH}/${suspended_file} /home/jenkins/suspended_users/
        
        rm -rf ${PARENT_PATH}/${dormant_file}
        rm -rf ${PARENT_PATH}/${suspended_file}
        '''.stripIndent())
}


publishers {
        extendedEmail {
            recipientList('Kanigeri_Shirisha@gap.com')
            defaultSubject('Suspending Dormant Users From GitHub')
            defaultContent('List of users who are suspended')
            contentType('text/html')
            triggers {
                success {
                    
                    subject('Suspending Dormant Users From GitHub')
                    content('Hello PIPES Team,<br><br>GitHub dormant users are suspended and can refer to the file that is attached<br><br>Regards,<br>GitHub Housekeeping.' )
                    sendTo { recipientList() }
                }

    
            }
        }

        downstream('send-email-to-suspended-users', 'SUCCESS')
    }

label('pipes-docker-agent')
}



