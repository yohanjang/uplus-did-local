#!/usr/bin/env groovy

/*
 * 해당 Jenkins Pipeline 은 Declarative 가 아닌 Scripted 문법으로 작성함
 * - Declarative 문법으로 작성하면 같은 Level 의 공통 Pipeline 에서 해당 Pipeline 을 호출시
 *   'java.lang.IllegalStateException: Only one pipeline { ... } block can be executed in a single run.' Error 발생
 * - Declarative 문법으로 해당 Error 를 회피하기 위해서는 CD (배포) Job 을 별도로 구현해야 함
 */

/*
 * [ MICM ] Kustomize/GitOps 기반 배포 실행 공통 pipeline
 */
def call (Map args) {

echo "======================================================================================================================"
echo "========++++++++ '${this.toString().take(this.toString().lastIndexOf('@'))}' 실행 Arguments ::: START ++++++++========"
echo args.toString()
echo "========++++++++ '${this.toString().take(this.toString().lastIndexOf('@'))}' 실행 Arguments ::: END ++++++++=========="
echo "======================================================================================================================"

node {

try {

  withEnv([

    // (TO-BE) CI Job 실행 Option : 현재는 해당 Parameter 를 사용하는 기능이 없으나 확장성을 위해 지정
    "CI_JOB_OPTION=${args.CI_JOB_OPTION}"

    /*
     * 대상 Application / Profile 관련 정보
     */

  , "APPLICATION_NAME=${args.APPLICATION_NAME}"

    // APP_PROFILE : 'dev', 'stg', 'prd' 중의 하나
    //               Deploy Repository 내 (MANIFEST_PATH 경로)/overlays 하위의 profile 경로로도 사용함
  , "APP_PROFILE=${args.APP_PROFILE}"

    /*
     * Deploy Repository 의 Git Clone 관련 정보
     */

    // Deploy Repository 의 Git URL
  , "DEPLOY_REPOSITORY_GIT_URL=http://18.220.186.237:9000/lgu-did/infra.git"

    // Deploy Repository 의 branch 이름 : 'main' branch 로 고정
    // Reference: https://codefresh.io/blog/stop-using-branches-deploying-different-gitops-environments/
  , "DEPLOY_REPOSITORY_TARGET_BRANCH_NAME=refs/heads/main"

    // Deploy Repository 의 Git Clone 디렉토리명
  , "DEPLOY_REPOSITORY_CLONE_DIRECTORY_NAME=manifest"

    // Deploy Repository 관련 Jenkins credentialsId : git clone / commit / push 등 에 사용
  , "DEPLOY_REPOSITORY_GIT_CREDENTIALS_ID=micm-cicd-user-git-credentials"

    /*
     * Deploy Repository 의 Manifet 수정에 필요한 정보
     */

    // Deploy Repository 내 해당 Application 의 Manifest 경로
  , "MANIFEST_PATH=${args.MANIFEST_PATH}"

    // Deploy Repository 의 해당 Application 의 manifest 에 지정된 해당 Image 의 Name ( ex : lgu-did/service-backend )
    // 해당 Image Name 은 실제로 존재하는 Image URL 이 아니며, CD pipeline 에서 실제 배포할 Image 정보 반영시 사용함
  , "IMAGE_NAME_IN_MANIFEST=${args.IMAGE_NAME_IN_MANIFEST}"

    // K8S (OpenShift) 에서 새로운 Image 를 pull 로 가져올 Private Image Registry (Harbor) URL
  , "NEW_IMAGE_PULL_REGISTRY_URL=nexus.example.com:5000"

    // 새로운 Image 이름 ( ex : dmicma/service-backend-dev )
  , "NEW_IMAGE_NAME=${args.NEW_IMAGE_NAME}"

    // manifest 에 반영할 새로운 빌드 버전
  , "NEW_BUILD_VERSION=${args.NEW_BUILD_VERSION}"

    // manifest 정보 수정시 git config user.name ~ , git config user.email ~ 로 지정할 User 정보
  , "DEPLOY_REPOSITORY_GIT_USER_NAME=micm-cicd"
  , "DEPLOY_REPOSITORY_GIT_USER_EMAIL=micm-cicd@example.com"

    // Deploy Repository 에 manifest 정보 수정시 commit message
  , "MANIFEST_COMMIT_MESSAGE=[Edit By Jenkins] ${args.NEW_IMAGE_NAME}:${args.NEW_BUILD_VERSION}"

  ]) {

    stage('Deploy Repository Clone') {

      echo "============================================================================================================="
      echo "========++++++++ Application '${env.APPLICATION_NAME}' ::: Deploy Repository Clone ::: START ++++++++========"
      echo "============================================================================================================="

      try {

        cleanWs()   // Clean before build

        script {

          checkout([
            $class: 'GitSCM', branches: [[name: "${env.DEPLOY_REPOSITORY_TARGET_BRANCH_NAME}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
              [ $class: 'CloneOption', shallow: true, depth: 1, timeout: 60 ],
              [ $class: 'RelativeTargetDirectory', relativeTargetDir: "${env.DEPLOY_REPOSITORY_CLONE_DIRECTORY_NAME}"],
              [ $class: 'SparseCheckoutPaths', sparseCheckoutPaths: [
                [ $class: 'SparseCheckoutPath', path: "${env.MANIFEST_PATH}" ],
              ] ]
            ],
            userRemoteConfigs: [
              [ credentialsId: "${env.DEPLOY_REPOSITORY_GIT_CREDENTIALS_ID}", 
                url: "${env.DEPLOY_REPOSITORY_GIT_URL}" ]
            ]
          ])

        }   // End of script

      } catch (e) {

        echo '======================================================================'
        echo '[ ERROR ]  Deploy Repository Clone ::: Failed !!!'
        echo '======================================================================'

        currentBuild.result = 'FAILURE'
        throw e

      }   // End of catch

      echo '======================================================================'
      echo '========++++++++ Deploy Repository Clone ::: END ++++++++========'
      echo '======================================================================'

    }   // End of 'Deploy Repository Clone' stage

    stage('Edit Manifest') {

      echo "==============================================================="
      echo "===========++++++++ Edit Manifest ::: START ++++++++==========="
      echo "==============================================================="

      try {

        // (해당 Jenkins Job Workspace 경로)/(Deploy Repository Clone 경로)/(해당 Application 의 Manifest 경로)/overlays/(APP_PROFILE)
        dir("${env.WORKSPACE}/${env.DEPLOY_REPOSITORY_CLONE_DIRECTORY_NAME}/${env.MANIFEST_PATH}/overlays/${env.APP_PROFILE}") {

          // 신규 image 의 URL, image name, version 을 해당 overlays profile 의 kustomization.yaml 에 반영
          sh """
          kustomize edit set image \
            "${env.IMAGE_NAME_IN_MANIFEST}=${env.NEW_IMAGE_PULL_REGISTRY_URL}/${env.NEW_IMAGE_NAME}:${env.NEW_BUILD_VERSION}"
          """

          echo "============================================="
          echo "[ INFO ]  kustomize build 결과 ::: START"
          echo "============================================="

          sh """
          kustomize build .
          """

          echo "============================================="
          echo "[ INFO ]  kustomize build 결과 ::: END"
          echo "============================================="

          echo "======================================================================"
          echo "[ INFO ]  Deploy Repository 에 변경된 Manifest 내용 Push ::: START"
          echo "======================================================================"

          withCredentials([gitUsernamePassword(
            credentialsId: "${env.DEPLOY_REPOSITORY_GIT_CREDENTIALS_ID}"
          )]) {

            sh """
            git pull origin HEAD --no-rebase

            git config user.name "${env.DEPLOY_REPOSITORY_GIT_USER_NAME}"
            git config user.email "${env.DEPLOY_REPOSITORY_GIT_USER_EMAIL}"

            git add \
              ./kustomization.yaml

            git commit \
              -m "${env.MANIFEST_COMMIT_MESSAGE}"

            git push origin HEAD:"${DEPLOY_REPOSITORY_TARGET_BRANCH_NAME}"
            """

          }   // End of withCredentials

          echo "======================================================================"
          echo "[ INFO ]  Deploy Repository 에 변경된 Manifest 내용 Push ::: END"
          echo "======================================================================"

        }   // End of dir

      } catch (e) {

        echo '======================================================='
        echo '[ ERROR ]  Edit Manifest ::: Failed !!!'
        echo '======================================================='

        currentBuild.result = 'FAILURE'
        throw e

      }   // End of catch

      echo "==============================================================="
      echo "===========++++++++ Edit Manifest ::: END ++++++++==========="
      echo "==============================================================="

    }  // End of 'Edit Manifest' stage

  }   // End of withEnv

} catch (err) {

  echo '===========++++++++ [ ERROR ] ++++++++==========='
  echo "$err"
  echo '================================================='

} finally {

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

}   // End of finally

}   // End of node

}   // End of call
