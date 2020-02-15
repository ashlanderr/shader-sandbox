// !devServer is an indicator of production build.
if (!config.devServer) {
    // Enable webpack optimizations.
    config.mode = 'production';
    config.devtool = false;

    // Remove sources maps from production bundle.
    const mapsIndex = config.module.rules.findIndex(r => r.use[0] === 'kotlin-source-map-loader');
    config.module.rules.splice(mapsIndex, 1);
}
