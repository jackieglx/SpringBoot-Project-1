pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        DOCKER_IMAGE = 'lynn513/student-management-project'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = 'dockerhub-credentials'
        EC2_SSH_CREDENTIALS = 'ec2-ssh-key'
        EC2_USER = 'ec2-user'
        EC2_HOST = '35.160.178.69'
        CONTAINER_NAME = 'student-management-project'
        EC2_ENV_FILE = '/home/ec2-user/student-management.env'
        APP_PORT = '8080'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                      -t ${DOCKER_IMAGE}:latest \
                      .
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKERHUB_CREDENTIALS}",
                    usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_TOKEN'
                )]) {
                    sh '''
                        echo "${DOCKERHUB_TOKEN}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy To EC2') {
            steps {
                sshagent(credentials: ["${EC2_SSH_CREDENTIALS}"]) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} "
                            set -e

                            if [ ! -f ${EC2_ENV_FILE} ]; then
                                echo 'Missing ${EC2_ENV_FILE}. Create it on EC2 before deployment.'
                                exit 1
                            fi

                            NETWORK_NAME=\\$(docker inspect student-postgres --format '{{range \\$name, \\$network := .NetworkSettings.Networks}}{{println \\$name}}{{end}}' 2>/dev/null | head -n 1)
                            if [ -z \\\"\\$NETWORK_NAME\\\" ]; then
                                NETWORK_NAME=student-management-network
                                docker network inspect \\\"\\$NETWORK_NAME\\\" >/dev/null 2>&1 || docker network create \\\"\\$NETWORK_NAME\\\"
                            fi

                            echo "Deploying ${DOCKER_IMAGE}:${DOCKER_TAG}"
                            docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true
                            docker run -d \\
                                --name ${CONTAINER_NAME} \\
                                --restart unless-stopped \\
                                --network \\\"\\$NETWORK_NAME\\\" \\
                                --env-file ${EC2_ENV_FILE} \\
                                -p ${APP_PORT}:8080 \\
                                ${DOCKER_IMAGE}:${DOCKER_TAG}

                            docker image prune -f
                            docker ps --filter name=${CONTAINER_NAME}
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Build, Docker push, and EC2 deployment completed.'
        }
        failure {
            echo 'Pipeline failed. Check the stage log above.'
        }
    }
}
