def isDocOnlyChanges() {
    for (changeLogSet in currentBuild.changeSets) {
        for (entry in changeLogSet.getItems()) { // for each commit in the detected changes
            for (file in entry.getAffectedFiles()) {
                filename = file.getPath();
                // if the file is part of the documentation set, skip it
                if (!filename.startsWith('documentation/')) {
                    // if the file is a markdown file, skip it
                    if (!filename.endsWith(".md")) {
                        // since the two previous conditions were not met, this change includes more than documentation
                        println filename
                        return false
                    }
                }
            }
        }
    }
    return true
}

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
        // timer trigger for "nightly build" on main branch
        cron( env.BRANCH_NAME.equals('main') ? 'H H(0-3) * * 1-5' : '')
    }

    environment {
        // variables for SystemTest stages (integration tests)
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
                not { changelog '\\[skip-ci\\].*' }
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
                    changeRequest target: 'main'
                    // if documentation only change, skip integration tests
                    not { expression { isDocOnlyChanges() } }
                    // if running nightly full integration tests, skip gate tests that are included in nightly
                    not { changelog '\\[full-mats\\].*' }
                    not { triggeredBy 'TimerTrigger' }
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh '''
                        cd tests
                        mvn clean verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate -DskipITs=false
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
                    tag 'release-*'
                    changelog '\\[full-mats\\].*'
                }
            }
            steps {
                script {
                    docker.image("${env.DB_IMAGE}").pull()
                }
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'otn-cred', passwordVariable: 'ORACLE_SUPPORT_PASSWORD', usernameVariable: 'ORACLE_SUPPORT_USERNAME']]) {
                    sh '''
                        cd tests
                        mvn clean verify -Dtest.staging.dir=${STAGING_DIR} -Dtest.groups=gate,nightly -DskipITs=false
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
                    branch "main"
                }
            }
            steps {
                sh '''
                    oci os object put --namespace=weblogick8s --bucket-name=wko-system-test-files --config-file=/dev/null --auth=instance_principal --force --file=installer/target/imagetool.zip --name=imagetool-main.zip
                '''
            }
        }
     }
}