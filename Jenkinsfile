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
                    not { changelog '\\[skip-ci\\].*' }
                    not { changelog '\\[full-mats\\].*' }
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
        stage ('Analyze') {
            when {
                anyOf {
                    changeRequest()
                    branch "main"
                }
            }
            tools {
                maven 'maven-3.6.0'
                jdk 'jdk11'
            }
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'encj_github_token', variable: 'GITHUB_TOKEN')]) {
                        runSonarScanner()
                    }
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

void runSonarScanner() {
    def changeUrl = env.GIT_URL.split("/")
    def org = changeUrl[3]
    def repo = changeUrl[4].substring(0, changeUrl[4].length() - 4)
    if (env.CHANGE_ID != null) {
        sh "mvn -B sonar:sonar \
            -Dsonar.projectKey=${org}_${repo} \
            -Dsonar.pullrequest.provider=GitHub \
            -Dsonar.pullrequest.github.repository=${org}/${repo} \
            -Dsonar.pullrequest.key=${env.CHANGE_ID} \
            -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
            -Dsonar.pullrequest.base=${env.CHANGE_TARGET}"
    } else {
       sh "mvn -B sonar:sonar \
           -Dsonar.projectKey=${org}_${repo} \
           -Dsonar.branch.name=${env.BRANCH_NAME}"
    }
}
