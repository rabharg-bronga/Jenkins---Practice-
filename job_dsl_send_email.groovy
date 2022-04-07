job('send-email-to-suspended-users') {

authorization {
    permission('hudson.model.Item.Build:authenticated')
    permission('hudson.model.Item.Cancel:authenticated')
}

environmentVariables {
    propertiesFile('mails.properties')
    keepBuildVariables(true)
}


steps {
    shell('''\
        #!/bin/bash
        cd /home/jenkins/suspended_users
        MAIL_IDS=$(tail -n +2 suspended_users.csv | cut -d, -f 1 | sed 's/^S-.*//' | tr -s '\n' ' ' | sed 's/ /@gap.com, /g' | sed 's/,.$//')
        echo MAIL_IDS=$MAIL_IDS > ${WORKSPACE}/mails.properties
        echo $MAIL_IDS
        cat ${WORKSPACE}/mails.properties
        '''.stripIndent())
}


publishers {
        extendedEmail {
            recipientList('$MAIL_IDS')
            defaultSubject('ALERT!! GitHub account suspended ')
            defaultContent('Ways to Reactivate/Unsuspend the account')
            contentType('text/html')
            triggers {
                success {
                    
                    subject('ALERT!! Your GitHub account suspended')
                    content('Hi GitHub User,<br><br>Your GitHub account is suspended because you are inactive since 90 days on Github. If you want to reactivate please refer the confluence page <br> https://confluence.gapinc.com/display/CICD/How+to+re-activate+suspended+account <br><br>Regards,<br>CI-CD Platform Team.' )
                    sendTo { recipientList() }
                }
            }
        }
    }
    
label('pipes-docker-agent')

}
