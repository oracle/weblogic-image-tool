pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
    tools {
        maven 'maven-3.6.0'
        jdk 'jdk11'
    }

    triggers {
        // timer trigger for "nightly build" on master branch
        cron( env.BRANCH_NAME.equals('master') ? 'H H(0-3) * * 1-5' : '')
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
                    sh 'mvn verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate'
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
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh 'mvn verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate,nightly'
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