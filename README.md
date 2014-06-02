SASS/SCSS Compass Grails Asset Pipeline
=======================================
The Grails `sass-asset-pipeline` is a plugin that provides SASS/SCSS support for the asset-pipeline static asset management plugin via the Compass framework.

For more information on how to use asset-pipeline, visit [here](http://www.github.com/bertramdev/asset-pipeline).


Usage
-----

Simply create files in your standard `assets/stylesheets` folder with extension `.scss` or `.sass`. You also may require other files by using the following requires syntax at the top of each file or the standard SASS import:

```css
/*
*= require test
*= require_self
*= require_tree .
*/

/*Or use this*/
@import 'test'

```

Including Sass files into your GSP files is easy but there are a few things worth mentioning. Say we have a file called `application.scss`. You would include it into your gsp by its compiled extension instead of its original extension. aka, use `.css` instead of `.scss`

```gsp
<head>
  <asset:stylesheet src="application.css"/>
</head>
```

External Configuration
----------------------
By default the `sass-asset-pipeline` will follow behavior from compass and look for a config.rb within the same folder as your required SASS file. This is not required, however, and is entirely optional. As a helper , we automatically add a global ruby variable referencing the plugin paths for all installed plugins of your application. This can be accessed in your config.rb if needed like so...

```ruby
$ui_path = PLUGIN_CONTEXT_PATHS["bertram-ui"]
```

Moreover it's possible to access the environment variable in config.rb to define environment specific config

```ruby
if Compass.configuration.environment == :production
  sass_options = {:debug_info => false}
  line_comments = false
else
  sass_options = {:debug_info => true}
  line_comments = true
end
```

Production
----------
During war build your sass files are compiled into css files. This is all well and good but sometimes you dont want each individual sass file compiled, but rather your main base sass file. It may be best to add a sub folder for those SASS files and exclude it in your precompile config...

Config.groovy:
```groovy
grails.assets.excludes = ["mixins/*.scss"]
```
