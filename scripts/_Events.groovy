// includeTargets << new File(assetPipelinePluginDir, "scripts/_AssetCompile.groovy")

eventAssetPrecompileStart = { assetConfig ->
	// def lessAssetFile = classLoader.loadClass('asset.pipeline.less.LessAssetFile')
	assetConfig.specs << 'asset.pipeline.sass.SassAssetFile'
}
