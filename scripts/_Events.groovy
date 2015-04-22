eventAssetPrecompileStart = { assetConfig ->
	def configHolder = classLoader.loadClass('asset.pipeline.AssetPipelineConfigHolder')
	if(configHolder.config == null) {
        configHolder.config = [:]
    }

    if(configHolder.config.sass == null) {
        configHolder.config.sass = [:]
    }
    configHolder.config.sass.resolveGems = false
}
// Remove the JRuby jar and gems directory before the war is bundled
eventCreateWarStart = { warName, stagingDir ->
	if (grailsEnv == "production") {
		grailsConsole.log "Removing JRuby jar and gems used for Sass compilation in Dev"
		Ant.delete(file:"${stagingDir}/WEB-INF/lib/jruby-complete-1.7.11.jar")
		Ant.delete(file:"${stagingDir}/WEB-INF/lib/jruby-container-0.4.0.jar")
		Ant.delete(dir:"${stagingDir}/WEB-INF/classes/gems")
	}
}
