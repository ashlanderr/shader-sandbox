// Enables css and sass loaders so you can require css from js/kt files.
config.module.rules.push({
  test: /\.css$/i,
  use: ['style-loader', 'css-loader'],
}, {
  test: /\.s[ac]ss$/i,
  use: ['style-loader', 'css-loader', 'sass-loader'],
});
