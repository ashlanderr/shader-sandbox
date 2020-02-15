// devServer is an indicator of development build.
if (config.devServer) {
    // Changes main webpack entry. More information in `compileKotlinJs` task in build.gradle.
    const projectName = config.output.filename.replace('.js', '');
    config.entry = config.entry.map(
        s => s.replace(`/js/packages/${projectName}/kotlin/`, "/js/entry/kotlin/"),
    );
}
