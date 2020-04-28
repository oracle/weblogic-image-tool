pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk11'
    }

    environment {
        STAGING_DIR = "/scratch/artifacts/imagetool"
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
            when {
                changeRequest()
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh 'mvn verify -Dtest.staging.dir=${STAGING_DIR}'
                }
            }
            post {
                always {
                    junit 'tests/target/failsafe-reports/*.xml'
                }
            }
        }
     }
}