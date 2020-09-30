pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk8'
    }

    triggers {
        // timer trigger for "nightly build" on master branch
        cron( env.BRANCH_NAME.equals('master') ? 'H H(0-3) * * 1-5' : '')
    }

    environment {
        STAGING_DIR = "/scratch/artifacts/imagetool"
        DB_IMAGE = "phx.ocir.io/weblogick8s/database/enterprise:12.2.0.1-slim"
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
                sh 'mvn -B -DskipTests -DskipITs clean install'
            }
        }
        stage ('Test') {
            when {
                not {
                    changelog '\\[skip-ci\\]'
                }
            }
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'imagetool/target/surefire-reports/*.xml'
                }
            }
        }
        stage ('SystemTest Gate') {
            when {
                allOf {
                    changeRequest target: 'master'
                    not {
                        changelog '\\[skip-ci\\]'
                    }
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh '''
                        cd tests
                        mvn clean verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate
                    '''
                }
            }
            post {
                always {
                    junit 'tests/target/failsafe-reports/*.xml'
                }
            }
        }
        stage ('SystemTest Full') {
            when {
                anyOf {
                    triggeredBy 'TimerTrigger'
                    tag "release-*"
                }
            }
            steps {
                script {
                    docker.image($DB_IMAGE).pull()
                }
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh '''
                        cd tests
                        mvn clean verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate,nightly
                    '''
                }
            }
            post {
                always {
                    junit 'tests/target/failsafe-reports/*.xml'
                }
                failure {
                    mail to: "${env.WIT_BUILD_NOTIFICATION_EMAIL_TO}", from: 'noreply@oracle.com',
                    subject: "WebLogic Image Tool: ${env.JOB_NAME} - Failed",
                    body: "Job Failed - \"${env.JOB_NAME}\" build: ${env.BUILD_NUMBER}\n\nView the log at:\n ${env.BUILD_URL}\n"
                }
            }
        }
        stage ('Save Nightly Installer'){
            when {
                allOf {
                    triggeredBy 'TimerTrigger'
                    branch "master"
                }
            }
            steps {
                sh '''
                    oci os object put --namespace=weblogick8s --bucket-name=wko-system-test-files --config-file=/dev/null --auth=instance_principal --force --file=installer/target/imagetool.zip --name=imagetool-master.zip
                '''
            }
        }
     }
}