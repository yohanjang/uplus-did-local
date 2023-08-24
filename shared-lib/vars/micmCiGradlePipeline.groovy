#!/usr/bin/env groovy

/*
 * [ MICM ] SpringBoot/Gradle 기반 Application 공통 CI pipeline
 */

def call (Map args) {

echo "======================================================================================================================"
echo "========++++++++ '${this.toString().take(this.toString().lastIndexOf('@'))}' 실행 Arguments ::: START ++++++++========"
echo args.toString()
echo "========++++++++ '${this.toString().take(this.toString().lastIndexOf('@'))}' 실행 Arguments ::: END ++++++++========"
echo "======================================================================================================================"

// Jenkins Job 시작 시간 : environment 내에 선언시 변수 값을 조회할 때마다 sh 를 호출하여 pipeline 외부에 별도 선언
def BUILD_DATETIME = """${ new Date().format('yyyyMMdd-HHmmss', TimeZone.getTimeZone('Asia/Seoul')) }"""

// Pipeline 실행 취소 flag
def AUTO_CANCELLED = false

/*
 * 개발, 검증, 운영 환경별 JOB Parameter 구분 ::: START
 */
switch (args.APP_PROFILE) {

  case 'dev':

    properties([
      parameters([
        choice(
            choices: ['OPTION_2', 'OPTION_1'], 
            name: 'CI_JOB_OPTION' ,
            description: '''
              [ 개발 ] 'OPTION_1' : CI 실행 Only ,
              'OPTION_2' : CI 실행 후 신규 Image 버전으로 Manifest 수정\n
            '''
        ),
        // [ $class: 'ChoiceParameter', choiceType: 'PT_RADIO',
        //   randomName: '', filterable: false, // filterLength: 1,
        //   name: 'CI_JOB_OPTION', description: '[ 개발 ]  CI Job 실행 Option',
        //   script: [
        //     $class: 'GroovyScript',
        //     script: [
        //       classpath: [], sandbox: true,
        //       script: '''return [
        //         'OPTION_1' : '[OPTION_1] CI 실행 Only',
        //         'OPTION_2' : '[OPTION_2] CI 실행 후 신규 Image 버전으로 Manifest 수정:selected',
        //       ]'''
        //     ]
        //   ]
        // ]
      ]),   // End of parameters
    ])   // End of properties

  break

  case 'stg':

    properties([
      parameters([
        choice(
            choices: ['OPTION_2', 'OPTION_1'], 
            name: 'CI_JOB_OPTION' ,
            description: '''
              [ 검증 ] 'OPTION_1' : CI 실행 Only ,
              'OPTION_2' : CI 실행 후 신규 Image 버전으로 Manifest 수정\n
            '''
        ),
        // [ $class: 'ChoiceParameter', choiceType: 'PT_RADIO',
        //   randomName: '', filterable: false, // filterLength: 1,
        //   name: 'CI_JOB_OPTION', description: '[ 검증 ]  CI Job 실행 Option',
        //   script: [
        //     $class: 'GroovyScript',
        //     script: [
        //       classpath: [], sandbox: true,
        //       script: '''return [
        //         'OPTION_1' : '[OPTION_1] CI 실행 Only',
        //         'OPTION_2' : '[OPTION_2] CI 실행 후 신규 Image 버전으로 Manifest 수정:selected',
        //       ]'''
        //     ]
        //   ]
        // ]
      ]),   // End of parameters
    ])   // End of properties

  break

  case 'prd':

    properties([
      parameters([
        choice(
            choices: ['OPTION_2', 'OPTION_1'], 
            name: 'CI_JOB_OPTION' ,
            description: '''
              [ 운영 ] 'OPTION_1' : CI 실행 Only ,
              'OPTION_2' : CI 실행 후 신규 Image 버전으로 Manifest 수정\n
            '''
        ),
        // [ $class: 'ChoiceParameter', choiceType: 'PT_RADIO',
        //   randomName: '', filterable: false, // filterLength: 1,
        //   name: 'CI_JOB_OPTION', description: '[ 운영 ]  CI Job 실행 Option',
        //   script: [
        //     $class: 'GroovyScript',
        //     script: [
        //       classpath: [], sandbox: true,
        //       script: '''return [
        //         'OPTION_1' : '[OPTION_1] CI 실행 Only',
        //         'OPTION_2' : '[OPTION_2] CI 실행 후 신규 Image 버전으로 Manifest 수정:selected',
        //       ]'''
        //     ]
        //   ]
        // ]
      ]),   // End of parameters
    ])   // End of properties

  // break

}   // End of switch
/*
 * 개발, 검증, 운영 환경별 JOB Parameter 구분 ::: END
 */

/*
 * 개발, 검증, 운영 환경별 고정 Parameter 구분 ::: START
 */
// CI/CD 대상 branch
def SOURCE_REPOSITORY_TARGET_BRANCH_NAME = ''

switch (args.APP_PROFILE) {

  case 'dev':
    // (TO-BE) 추후 develop 으로 수정 예정
    SOURCE_REPOSITORY_TARGET_BRANCH_NAME = 'main'

  break;

  case 'stg':
    SOURCE_REPOSITORY_TARGET_BRANCH_NAME = 'main'

  break;

  case 'prd':
    // (TO-BE) 브랜치 전략 확정 후 필요시 GIT_CI_TAG 값 반영 예정
    SOURCE_REPOSITORY_TARGET_BRANCH_NAME = 'main'

  break;

}
/*
 * 개발, 검증, 운영 환경별 고정 Parameter 구분 ::: END
 */

/*
 * 각 Application 별 고정 Parameter 구분 ::: START
 */
// 해당 Application 의 Source 가 위치한 Git URL
def SOURCE_REPOSITORY_GIT_URL = ''

switch (args.APPLICATION_NAME) {

  case 'service-backend':
    SOURCE_REPOSITORY_GIT_URL = 'http://18.220.186.237:9000/lgu-did/service-backend.git'

  break;

  case 'solution-backend':
  case 'solution-registry':
    SOURCE_REPOSITORY_GIT_URL = 'http://18.220.186.237:9000/lgu-did/solution-backend.git'

  break;

}
/*
 * 각 Application 별 고정 Parameter 구분 ::: END
 */

pipeline {

  agent any

  environment {

    /*
     * 대상 Application / Profile / 빌드 버전 정보
     */

    APPLICATION_NAME = "${args.APPLICATION_NAME}"

    // APP_PROFILE : 'dev', 'stg', 'prd' 중의 하나
    //               Deploy Repository 내 (MANIFEST_PATH 경로)/overlays 하위의 profile 경로로도 사용함
    APP_PROFILE = "${args.APP_PROFILE}"

    // 새로운 빌드 버전
    // - GIT_CI_TAG Parameter 값 전달시   : "${params.GIT_CI_TAG}-${BUILD_DATETIME}" 형식으로 Unique 한 값으로 지정
    // - GIT_CI_TAG Parameter 값 미전달시 : Jenkins Job 시작 시간 기반으로  Unique 한 값으로 지정
    // NEW_BUILD_VERSION = "${BUILD_DATETIME}"
    NEW_BUILD_VERSION = "${ params.GIT_CI_TAG != null && params.GIT_CI_TAG != '' ? "${params.GIT_CI_TAG}-${BUILD_DATETIME}" : "${BUILD_DATETIME}" }"

    /*
     * Source Repository 의 Git Clone 관련 정보
     */

    // 해당 Application 의 Source 가 있는 Git Repository URL
    // pipeline block 바깥에서 def 로 선언한 변수를 Environment Variable 로 다시 지정
    SOURCE_REPOSITORY_GIT_URL = "${SOURCE_REPOSITORY_GIT_URL}"

    // CI/CD 대상 branch
    // pipeline block 바깥에서 def 로 선언한 변수를 Environment Variable 로 다시 지정
    SOURCE_REPOSITORY_TARGET_BRANCH_NAME = "${SOURCE_REPOSITORY_TARGET_BRANCH_NAME}"

    // Source Repository 의 Git Clone 절대 경로 : (해당 Job 작업 경로)/(Application 명)-(새로운 빌드 버전)
    SOURCE_REPOSITORY_CLONE_FULL_PATH = "${env.WORKSPACE}/${env.APPLICATION_NAME}-${env.NEW_BUILD_VERSION}"

    // Source Repository 관련 Jenkins credentialsId : git clone / commit / push 등 에 사용
    SOURCE_REPOSITORY_GIT_CREDENTIALS_ID = "${ params.SOURCE_REPOSITORY_GIT_CREDENTIALS_ID ? params.SOURCE_REPOSITORY_GIT_CREDENTIALS_ID : 'micm-source-repository-git-credentials' }"

    /*
     * Gradle 빌드 / Artifact 관련 정보
     */

    // Gradle 빌드 후 Artifact (jar) 생성 경로
    ARTIFACT_FILE_PATH = "${args.ARTIFACT_FILE_PATH}"

    // Gradle 빌드 후 생성되는 Artifact (jar) 파일명
    ARTIFACT_FILE_NAME = "${args.ARTIFACT_FILE_NAME}"

    // Nexus Maven Mirror Protocol, URL
    // - 해당 Mirror 는 https://repo.maven.apache.org/maven2/ (Maven), https://plugins.gradle.org/m2/ (Gradle Plugins)
    //   두 site 에 대한 Proxy/Cache 역할을 수행함
    MAVEN_MIRROR_PROTOCOL = 'http'
    MAVEN_MIRROR_URL = '172.28.5.254:9001/repository/maven-public/'

    // Nexus Maven Mirror 연동시 사용할 Jenkins credentialsId
    MAVEN_MIRROR_CREDENTIALS_ID = 'micm-nexus-maven-mirror-credentials'

    // GRADLE_USER_HOME : Gradle Build 에 필요한 dependencies 등이 저장되는 Jenkins 내 절대경로
    GRADLE_USER_HOME = '/jenkins_dependencies/.gradle'

    // Jenkins 내 Gradle 7.3 Binary 가 있는 절대경로
    GRADLE_7_3_INSTALL_PATH = '/usr/lib/gradle/gradle-7.3'

    // Gradle 빌드 전체 Command ( ex : gradle clean build -Dorg.gradle.daemon=false -Dorg.gradle.caching=false --exclude-task test )
    GRADLE_BUILD_FULL_COMMAND = 'gradle' + ' ' + "${ args.GRADLE_BUILD_COMMAND_LIST.join(' ') }"

    /*
     * base image 관련 정보
     */

    // Jenkins 에서 base image 를 pull 로 가져올 Private Image Registry (Harbor) URL
    BASE_IMAGE_REGISTRY_URL = "${ args.BASE_IMAGE_REGISTRY_URL ? args.BASE_IMAGE_REGISTRY_URL : 'nexus.example.com:5000' }"

    // base image 의 이름
    BASE_IMAGE_NAME = "${ args.BASE_IMAGE_NAME ? args.BASE_IMAGE_NAME : 'dmicma/ubi8-openjdk-17' }"

    // base image 의 버전
    BASE_IMAGE_VERSION = "${ args.BASE_IMAGE_VERSION ? args.BASE_IMAGE_VERSION : '1.16-2' }"

    // base image 의 Full URL
    BASE_IMAGE_FULL_URL = "${env.BASE_IMAGE_REGISTRY_URL}/${env.BASE_IMAGE_NAME}:${env.BASE_IMAGE_VERSION}"

    // base image Pull 시 사용할 Jenkins credentialsId
    BASE_IMAGE_PULL_CREDENTIALS_ID = 'micm-private-image-registry-credentials'

    /*
     * CI 성공시 생성되는 new image 관련 정보
     */

    // Gradle/Docker 빌드 후 생성된 새로운 Image 를 push 할 URL
    NEW_IMAGE_PUSH_REGISTRY_URL = 'nexus.example.com:5002'

    // 새로운 Image 이름 ( ex : dmicma/service-backend-dev )
    NEW_IMAGE_NAME = "${args.NEW_IMAGE_NAME}"

    // new image 의 Full URL
    NEW_IMAGE_PUSH_FULL_URL = "${env.NEW_IMAGE_PUSH_REGISTRY_URL}/${env.NEW_IMAGE_NAME}:${env.NEW_BUILD_VERSION}"

    // new image Push 시 사용할 Jenkins credentialsId
    NEW_IMAGE_PUSH_CREDENTIALS_ID = 'micm-private-image-registry-credentials'

    /*
     * Source Repository 에 Build Version 관련 CI Tag 값 push 관련 정보
     */

    // Build Version 관련 CI Tag 값 추가시 git config user.name ~ , git config user.email ~ 로 지정할 User 정보
    SOURCE_REPOSITORY_GIT_USER_NAME  = 'micm-cicd'
    SOURCE_REPOSITORY_GIT_USER_EMAIL = 'micm-cicd@example.com'

    // Source Repository 에 추가할 Build Version 관련 CI Tag 값
    SOURCE_REPOSITORY_BUILD_VERSION_TAG = "CI--${env.APP_PROFILE}--${env.NEW_BUILD_VERSION}"

    // Source Repository 에 Build Version 관련 CI Tag 추가시 commit message
    SOURCE_REPOSITORY_BUILD_VERSION_TAG_COMMIT_MESSAGE = "CI--${env.APP_PROFILE}--${env.NEW_BUILD_VERSION}"

    /*
     * CD (배포) 관련 정보
     */

    // Deploy Repository 내 해당 Application 의 Manifest 경로
    MANIFEST_PATH = "${args.MANIFEST_PATH}"

    // Deploy Repository 의 해당 Application 의 manifest 에 지정된 해당 Image 의 Name ( ex : lgu-did/service-backend )
    // 해당 Image Name 은 실제로 존재하는 Image URL 이 아니며, CD pipeline 에서 실제 배포할 Image 정보 반영시 사용함
    IMAGE_NAME_IN_MANIFEST = "${args.IMAGE_NAME_IN_MANIFEST}"

  }   // End of environment

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)   // This is required if you want to clean before build
    buildDiscarder(logRotator(numToKeepStr: '5'))
  }

  tools {
    jdk 'openjdk-17'
  }

  stages {

    stage("Init"){

        steps {
          cleanWs()   // Clean before build

          echo "======================================================"
          echo "============= Jenkins Job Info ::: START ============="

          echo "### Jenkins Job BUILD_NUMBER ::: '${env.BUILD_NUMBER}'"
          echo "### Jenkins Job 시작 시간    ::: '${BUILD_DATETIME}'"
          echo "### 새로운 빌드 버전         ::: '${env.NEW_BUILD_VERSION}'"
          echo "### CI_JOB_OPTION 값         ::: '${params.CI_JOB_OPTION}'"

          echo "============== Jenkins Job Info ::: END =============="
          echo "======================================================"

          script {

            if( params.CI_JOB_OPTION == '' || params.CI_JOB_OPTION == null || params.CI_JOB_OPTION.startsWith('[OPTION') ) {
              echo '======================================================================='
              echo '[ WARN ] CI_JOB_OPTION is Empty !!! ::: All stages will be skipped ...'
              echo '======================================================================='

              AUTO_CANCELLED = true
              currentBuild.result = 'ABORTED'

              return
            }

          }   // End of script

        }   // End of steps

    }   // End of 'Init' stage

    stage('Source Repository Clone') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      steps {
        echo "============================================================================================================="
        echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Source Repository Clone ::: START ++++++++========"
        echo "============================================================================================================="

        script {
          switch( env.APP_PROFILE ) {

            case 'dev':
            case 'stg':
            case 'prd':

              checkout([
                $class: 'GitSCM', branches: [[name: "${env.SOURCE_REPOSITORY_TARGET_BRANCH_NAME}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                  [ $class: 'CloneOption', shallow: true, depth: 1, timeout: 60 ],
                  [ $class: 'RelativeTargetDirectory', relativeTargetDir: "${env.SOURCE_REPOSITORY_CLONE_FULL_PATH}"]
                ],
                userRemoteConfigs: [
                  [ credentialsId: "${env.SOURCE_REPOSITORY_GIT_CREDENTIALS_ID}", 
                    url: "${env.SOURCE_REPOSITORY_GIT_URL}" ]
                ]
              ])

            break;

          }   // End of switch
        }   // End of script

        dir("${env.SOURCE_REPOSITORY_CLONE_FULL_PATH}") {
          echo "=========== Commit Log  ::: START ==========="

          script {
            // Commit ID (짧은 길이 커밋 해시)
            env.CURRENT_COMMIT_ID = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()

            // Commit Author
            env.CURRENT_COMMIT_AUTHOR = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%an'").trim()

            // Commit Time
            env.CURRENT_COMMIT_TIME= sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%ad' --date=format:'%Y-%m-%d %H:%M:%S %z'").trim()

            // Commit Message
            env.CURRENT_COMMIT_MESSAGE = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%s'").trim()
          }

          echo "### Commit ID      ::: '${env.CURRENT_COMMIT_ID}'"
          echo "### Commit Author  ::: '${env.CURRENT_COMMIT_AUTHOR}'"
          echo "### Commit Time    ::: '${env.CURRENT_COMMIT_TIME}'"
          echo "### Commit Message ::: '${env.CURRENT_COMMIT_MESSAGE}'"

          echo "=========== Commit Log  ::: END ==========="
        }   // End of dir
      }   // End of steps

      post {
        success {
          echo "================================================================="
          echo '========++++++++ Source Repository Clone ::: END ++++++++========'
          echo "================================================================="
        }
        failure {
          echo "================================================================="
          echo '[ ERROR ]  Source Repository Clone ::: Failed !!!'
          echo "================================================================="
        }
      }   // End of post

    }  // End of 'Source Repository Clone' stage

    stage('Pre-Work Before Gradle Build') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      steps {
        echo "=================================================================================================================="
        echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Pre-Work Before Gradle Build ::: START ++++++++========"
        echo "=================================================================================================================="

        dir("${SOURCE_REPOSITORY_CLONE_FULL_PATH}") {

          // (추후 필요시 구현) Gradle 빌드 전, Application 소스 수정 등 필요한 작업을 해당 step 에 명시할 수 있음
          sh '''
          echo "[ INFO ] 'Pre-Work Before Gradle Build' Stage ::: Skipping..."
          '''

        }   // End of dir
      }   // End of stpes

      post {
        success {
          echo '======================================================================'
          echo '========++++++++ Pre-Work Before Gradle Build ::: END ++++++++========'
          echo '======================================================================'
        }
        failure {
          echo '======================================================================'
          echo '[ ERROR ]  Pre-Work Before Gradle Build ::: Failed !!!'
          echo '======================================================================'
        }
      }   // End of post
    }   // End of 'Pre-Work Before Gradle Build' stage

    stage('Gradle Build') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      environment {
        // Gradle bin 경로를 PATH 에 추가
        PATH="${env.GRADLE_7_3_INSTALL_PATH}/bin:${env.PATH}"
      }

      steps {
        dir("${env.SOURCE_REPOSITORY_CLONE_FULL_PATH}") {
          echo "=================================================================================================="
          echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Gradle Build ::: START ++++++++========"
          echo "=================================================================================================="

          /* 
           * [ Jenkins 에서 Gradle 빌드시 Nexus 연동에 필요한 환경 변수 ]
           *
           * - MAVEN_MIRROR_PROTOCOL, MAVEN_MIRROR_URL : 상단 environment block 에서 정의하여 환경 변수로 선언
           * - MAVEN_MIRROR_USERNAME, MAVEN_MIRROR_PASSWORD : Jenkins credentials 의 값을 withCredentials 에서 환경변수로 선언
           * 
           * @see service-backend, solution-backend 의 settings.gradle
           */

          withCredentials([usernamePassword(
            credentialsId: "${env.MAVEN_MIRROR_CREDENTIALS_ID}" ,
            usernameVariable: 'MAVEN_MIRROR_USERNAME', passwordVariable: 'MAVEN_MIRROR_PASSWORD',
          )]) {

            // Gradle Build
            sh "${env.GRADLE_BUILD_FULL_COMMAND}"

          }   // End of withCredentials

        }   // End of dir
      }   // End of steps

      post {
        success {
          echo '======================================================'
          echo '========++++++++ Gradle Build ::: END ++++++++========'
          echo '======================================================'
        }
        failure {
          echo '======================================================'
          echo '[ ERROR ]  Gradle Build ::: Failed !!!'
          echo '======================================================'
        }
      }   // End of post

    }  // End of 'Gradle Build' stage

    stage('Pre-Work Before Docker Build') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      steps {
        dir("${SOURCE_REPOSITORY_CLONE_FULL_PATH}") {
          echo "=================================================================================================================="
          echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Pre-Work Before Docker Build ::: START ++++++++========"
          echo "=================================================================================================================="

          /*
           * [ Step 1. ]  Dockerfile 생성
           *
           * - (TO-BE) 현재 Dockerfile 은 Test 용으로 구현함 => 추후 실제 사용할 Dockerfile 적용 예정
           */

// [ Jenkinsfile 내 Dockerfile 작성 방법 ]
// - ${...} : Jenkinsfile 내 해당 변수의 값으로 치환됨
// - \\\${...} : 치환되지 않고 '${...}' 형식으로 Dockerfile 에 쓰여짐 => line break 문자를 3번 입력
// - '\\\\' : RUN 절에서 multi-line 의 줄바꿈 처리 => line break 문자를 4번 입력

// ubi8/openjdk-17 기반 Dockerfile ::: START

          sh """
tee ./Dockerfile << EOF
FROM ${env.BASE_IMAGE_FULL_URL}

# Docker build 시 전달된 jar 파일 경로 ::: HOST (ex: Jenkins CI 서버) 내 jar 파일 경로
ARG ARTIFACT_FILE_PATH

# Docker build 시 전달된 jar 파일명 ::: HOST (ex: Jenkins CI 서버) 내 jar 파일명
ARG ARTIFACT_FILE_NAME
ENV ARTIFACT_FILE_NAME="\\\${ARTIFACT_FILE_NAME}"

# HOST 의 jar 를 Image 내 INSTALL_PATH 경로 내 app.jar 로 COPY
COPY "\\\${ARTIFACT_FILE_PATH}"/"\\\${ARTIFACT_FILE_NAME}" /deployments/app.jar

EOF"""

// ubi8/openjdk-17 기반 Dockerfile ::: END

        }   // End of dir
      }   // End of steps

      post {
        success {
          echo '======================================================================'
          echo '========++++++++ Pre-Work Before Docker Build ::: END ++++++++========'
          echo '======================================================================'
        }
        failure {
          echo '======================================================================'
          echo '[ ERROR ]  Pre-Work Before Docker Build ::: Failed !!!'
          echo '======================================================================'
        }
      }   // End of post
    }   // End of 'Pre-Work Before Docker Build' stage

    stage('Docker Build/Push') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      steps {
        dir("${env.SOURCE_REPOSITORY_CLONE_FULL_PATH}") {

          echo "=================================================================================================="
          echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Docker Build ::: START ++++++++========"
          echo "=================================================================================================="

          // cleanup current user docker credentials
          sh '''
          sudo rm -rf /root/.dockercfg || true
          sudo rm -rf /root/.docker/config.json || true
          '''

          // Base Image Registry login
          withCredentials([usernamePassword(
            credentialsId: "${env.BASE_IMAGE_PULL_CREDENTIALS_ID}", 
            usernameVariable: 'B_USERNAME', passwordVariable: 'B_PASSWORD',
          )]) {

            // 'Warning: A secret was passed to "sh" using Groovy String interpolation, which is insecure.' 방지
            withEnv([
              "BASE_IMAGE_REGISTRY_URL=${env.BASE_IMAGE_REGISTRY_URL}"
            ]) {
              sh 'echo ${B_PASSWORD} | sudo docker login -u ${B_USERNAME} --password-stdin ${BASE_IMAGE_REGISTRY_URL}'
            }

          }   // End of withCredentials

          // Base Image Pull => Docker Build
          sh """
          sudo docker pull ${env.BASE_IMAGE_FULL_URL}

          sudo docker build \
            --rm \
            --force-rm \
            --no-cache \
            --tag "${env.NEW_IMAGE_PUSH_FULL_URL}" \
            --build-arg ARTIFACT_FILE_PATH=${env.ARTIFACT_FILE_PATH} \
            --build-arg ARTIFACT_FILE_NAME=${env.ARTIFACT_FILE_NAME} \
            -f ./Dockerfile \
            .

          sudo docker images
          """

          // New Image Registry login
          withCredentials([usernamePassword(
            credentialsId: "${env.NEW_IMAGE_PUSH_CREDENTIALS_ID}", 
            usernameVariable: 'N_USERNAME', passwordVariable: 'N_PASSWORD',
          )]) {

            // 'Warning: A secret was passed to "sh" using Groovy String interpolation, which is insecure.' 방지
            withEnv([
              "NEW_IMAGE_PUSH_REGISTRY_URL=${env.NEW_IMAGE_PUSH_REGISTRY_URL}"
            ]) {
              sh 'echo ${N_PASSWORD} | sudo docker login -u ${N_USERNAME} --password-stdin ${NEW_IMAGE_PUSH_REGISTRY_URL}'
            }

          }   // End of withCredentials

          // New Image Push
          sh """
          sudo docker push "${env.NEW_IMAGE_PUSH_FULL_URL}"
          """

        }   // End of dir
      }   // End of steps

      post {
        success {
          echo '======================================================'
          echo '========++++++++ Docker Build ::: END ++++++++========'
          echo '======================================================'
        }
        failure {
          echo '======================================================'
          echo '[ ERROR ]  Docker Build ::: Failed !!!'
          echo '======================================================'
        }
      }   // End of post

    }  // End of 'Docker Build/Push' stage

    stage('Add Build Version Tag') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
        }
      }

      steps {
        dir("${env.SOURCE_REPOSITORY_CLONE_FULL_PATH}") {

          echo "==========================================================================================================="
          echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Add Build Version Tag ::: START ++++++++========"
          echo "==========================================================================================================="

          script {

            withCredentials([gitUsernamePassword(
              credentialsId: "${env.SOURCE_REPOSITORY_GIT_CREDENTIALS_ID}"
            )]) {

              switch( env.APP_PROFILE ) {

                // 개발 환경 CI 의 경우 Source Repository 에 Build Version Tag 를 추가하지 않음
                case 'dev':
                  echo "[ INFO ]  'Add Build Version Tag' Stage ::: Skipping ..."

                break;

                case 'stg':

                  sh """
                  git config user.name "${SOURCE_REPOSITORY_GIT_USER_NAME}"
                  git config user.email "${SOURCE_REPOSITORY_GIT_USER_EMAIL}"

                  git tag \
                    -a "${SOURCE_REPOSITORY_BUILD_VERSION_TAG}" \
                    -m "${SOURCE_REPOSITORY_BUILD_VERSION_TAG_COMMIT_MESSAGE}" \
                    "${CURRENT_COMMIT_ID}"

                  git push origin HEAD:"${SOURCE_REPOSITORY_TARGET_BRANCH_NAME}" --tags
                  """

                break;

                case 'prd':

                  // 운영 환경 CI 의 경우, 명시적으로 main 브랜치에 tag 를 push 함
                  sh """
                  git config user.name "${SOURCE_REPOSITORY_GIT_USER_NAME}"
                  git config user.email "${SOURCE_REPOSITORY_GIT_USER_EMAIL}"

                  git tag \
                    -a "${SOURCE_REPOSITORY_BUILD_VERSION_TAG}" \
                    -m "${SOURCE_REPOSITORY_BUILD_VERSION_TAG_COMMIT_MESSAGE}" \
                    "${CURRENT_COMMIT_ID}"

                  git push origin HEAD:main --tags
                  """

                break;

              }   // End of switch

            }   // End of withCredentials

          }   // End of script
        }   // End of dir
      }   // End of steps

      post {
        success {
          echo '==============================================================='
          echo '========++++++++ Add Build Version Tag ::: END ++++++++========'
          echo '==============================================================='
        }
        failure {
          echo '==============================================================='
          echo '[ ERROR ]  Add Build Version Tag ::: Failed !!!'
          echo '==============================================================='
        }
      }   // End of post

    }   // End of 'Add Build Version Tag'

    stage('Trigger CD pipeline') {

      when {
        allOf {
          expression { AUTO_CANCELLED == false }
          // CI Job 실행 Option 값 'OPTION_2' 인 경우 실행
          expression { params.CI_JOB_OPTION == 'OPTION_2' }
        }
      }

      steps {

        echo "========================================================================================================="
        echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Trigger CD pipeline ::: START ++++++++========"
        echo "========================================================================================================="

        script {

          def pipelineJobArgs = [
              CI_JOB_OPTION          : "${params.CI_JOB_OPTION}"
            , APPLICATION_NAME       : "${env.APPLICATION_NAME}"
            , APP_PROFILE            : "${env.APP_PROFILE}"
            , MANIFEST_PATH          : "${env.MANIFEST_PATH}"
            , IMAGE_NAME_IN_MANIFEST : "${env.IMAGE_NAME_IN_MANIFEST}"
            , NEW_IMAGE_NAME         : "${env.NEW_IMAGE_NAME}"
            , NEW_BUILD_VERSION      : "${env.NEW_BUILD_VERSION}"
          ]

          // Kustomize/GitOps 기반 배포 실행 공통 pipeline 실행
          micmCdKustomizePipeline(pipelineJobArgs)

        }   // End of script

      }   // End of steps

      post {
        success {
          echo '==============================================================='
          echo '=========++++++++ Trigger CD pipeline ::: END ++++++++========='
          echo '==============================================================='
        }
        failure {
          echo '==============================================================='
          echo '[ ERROR ]  Execute CD pipeline ::: Failed !!!'
          echo '==============================================================='
        }
      }   // End of post

    }   // End of 'Trigger CD pipeline'

  }   // End of stages

  /*
  * Declarative: Post Actions
  */

  post {

    always {

      // CI 서버 내 빌드 완료된 Image 모두 삭제
      sh """
      sudo docker rmi -f "${BASE_IMAGE_FULL_URL}" || true
      sudo docker rmi -f "${NEW_IMAGE_PUSH_FULL_URL}" || true
      """

      // Jenkins workspace 정리
      cleanWs(
        cleanWhenSuccess: true,
        cleanWhenAborted: true,
        cleanWhenNotBuilt: true,
        cleanWhenUnstable: true,
        cleanWhenFailure: true,
        deleteDirs: true,
        notFailBuild: true,
        disableDeferredWipeout: true,
        patterns: [
          [pattern: '**/*', type: 'INCLUDE']
        ]
      )

      dir("${env.WORKSPACE}@tmp") {
        deleteDir()
      }
      dir("${env.WORKSPACE}@script") {
        deleteDir()
      }
      dir("${env.WORKSPACE}@script@tmp") {
        deleteDir()
      }
      dir("${env.WORKSPACE}@libs") {
        deleteDir()
      }

    }   // End of always

  }  // End of post
  
}   // End of pipeline

}   // End of call
