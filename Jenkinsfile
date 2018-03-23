pipeline {

    agent {
        label 'master'
    }

    tools {
        maven 'Maven3'
        jdk 'jdk9'
    }

    stages {
        stage("mvn build") {
            steps {
                sh 'mvn clean install'
                junit allowEmptyResults: true, testResults: '/target/surefire-reports/**/*.xml'

            }
        }

        stage ("sonar analysis") {
            steps {
                withSonarQubeEnv('Sonar') {
                    sh "${tool 'SonarScanner'}/bin/sonar-scanner"
                }
            }
        }
    }
}