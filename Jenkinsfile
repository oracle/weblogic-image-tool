pipeline {
    agent any
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk11'
    }

    stages {
        stage ('Environment') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                    mvn --version
                '''
            }
        }
        stage ('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage ('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'imagetool/target/surefire-reports/*.xml'
                }
            }
        }
        stage ('SystemTest') {
            steps {
                withCredentials([usernameColonPassword(credentialsId: 'otn-cred', variable: 'otnuser')]) {
                    echo '$otnuser'
                    echo 'hope you saw the userid'
                    sh 'mvn verify'
                }
            }
        }
     }
}