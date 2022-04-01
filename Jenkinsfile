pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
            }
        }
        stage("GoodBye"){
            steps{
                echo 'GoodBye World'
            }
        }
        stage("Verify Branch"){
            steps{
                echo '$GIT_BRANCH'
            }
        }
        
    }
}
