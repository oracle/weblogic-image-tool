pipeline {
    agent any

    stages {
        stage ('Initialize') {
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
                withMaven {
                    maven 'maven-3.6.0'
                    jdk 'jdk11'

                    sh 'mvn clean package'
                }
            }
        }
    }
}