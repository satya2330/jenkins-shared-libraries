#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    // Updated to match your actual Credential ID 'GIT-jenkins'
    def gitCredentials = config.gitCredentials ?: 'GIT-jenkins' 
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'satyadevops30@gmail.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // We use a single sh block for all commands
        sh """
            # 1. Configure Git
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            # 2. Update manifests
            sed -i "s|image: stephcurry30/easyshop-app:.*|image: stephcurry30/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: stephcurry30/easyshop-migration:.*|image: stephcurry30/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            # 3. Commit and Push
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                
                # IMPORTANT: No extra 'sh' here, and NO SPACE after https://
                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/satya2330/shoppingkartapplication.git master
            fi
        """
    }
}
