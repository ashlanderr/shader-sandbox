config.module.rules.push({
  test: /\.(ttf|eot|svg)$/,
  use: {
    loader: 'file-loader',
    options: {
      name: 'fonts/[hash].[ext]'
    }
  }
}, {
  test: /\.(woff|woff2)$/,
  use: {
    loader: 'url-loader',
    options: {
      name: 'fonts/[hash].[ext]',
      limit: 5000,
      mimetype: 'application/font-woff'
    }
  }
});
