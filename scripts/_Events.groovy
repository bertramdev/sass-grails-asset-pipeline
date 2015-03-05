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