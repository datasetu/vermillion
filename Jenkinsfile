node {

    stage('Initialize')
    {   def javaHome = tool 'openjdk17'
        def dockerHome = tool 'MyDocker'
        def mavenHome  = tool 'Maven3.8'
        env.PATH = "${dockerHome}/bin:${mavenHome}/bin:${env.PATH}"
    }



    stage('Build')
           {
            sh """
        docker build -t build_img .
    """
          }

    stage('Run')
           {
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
          }
}
