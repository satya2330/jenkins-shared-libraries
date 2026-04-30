#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags for Satyam's Environment
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    // Corrected to match your actual Jenkins Credential ID
    def gitCredentials = config.gitCredentials ?: 'GIT-jenkins'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'satyadevops30@gmail.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // We use a single sh block to prevent variable interpolation issues
        sh """
            # 1. Configure Git
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            # 2. Update deployment manifests (Using your Docker Hub: stephcurry30)
            sed -i "s|image: stephcurry30/easyshop-app:.*|image: stephcurry30/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: stephcurry30/easyshop-migration:.*|image: stephcurry30/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # Ensure ingress host is correct
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            # 3. Check for changes and Push
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                
                # FIXED: Removed the space and corrected the repository path
                # Using the environment variables $GIT_USERNAME and $GIT_PASSWORD directly
                git remote set-url origin https://\$GIT_USERNAME:\$GIT_PASSWORD@github.com/satya2330/shoppingkartapplication.git
                
                # Pushing specifically to master as shown in your logs
                git push origin master
            fi
        """
    }
}
