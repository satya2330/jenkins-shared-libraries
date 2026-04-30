#!/usr/bin/env groovy

def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-jenkins' // Matches your active credential ID
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'satyadevops30@gmail.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            # Configure Git
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            # Update main application deployment
            sed -i "s|image: stephcurry30/easyshop-app:.*|image: stephcurry30/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: stephcurry30/easyshop-migration:.*|image: stephcurry30/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # Update ingress host
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            # Check for changes and push
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                
                # REMOVED THE SPACE and switched to using the credential variables
                # Pushing to 'master' branch as seen in your logs
                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/satya2330/shoppingkartapplication.git master
            fi
        """
    }
}
