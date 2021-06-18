pipeline {
    agent {
        dockerfile true
    }
    tools {
            maven 'Maven3.8'
            jdk 'openjdk17'
        }

    stages {
    stage('Build') {
    steps {
    sh """
        docker build -t build_img .
    """
    }
    }
    stage('Run') {
        steps {
        sh """
        docker run --rm build_img
    """
    echo 'Set log level to "FINE" in apiserver'
    sh  "sed -i '' 's/INFO/FINEST/g' api-server/src/main/resources/vertx-default-jul-logging.properties"

    echo 'Set log level to "FINE" in authenticator'
    sh "sed -i '' 's/INFO/FINEST/g' authenticator/src/main/resources/vertx-default-jul-logging.properties"

    echo 'Compile api server source files'
    sh 'cd api-server && mvn clean package'

    echo 'Compile authenticator source files'
    sh 'cd authenticator && mvn clean package'


    echo 'Set vermillion env to "test" in conf file'
    sh "sed -i 's/VERMILLION_ENV=prod/VERMILLION_ENV=test/g' setup/vermillion.conf"

    echo 'Install Vermillion'
    sh "./setup/install"

    echo 'Wait for the middleware to be available'
    sh "cd setup && ./wait.sh"

    echo 'Load sample data into ES'
    sh "cd setup && ./load_sample_data.sh"

    echo 'Run tests and generate reports'
    sh "cd setup && ./coverage.sh"
    }
    }
   }
}
