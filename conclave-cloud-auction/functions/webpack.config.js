var path = require('path');
var webpack = require('webpack');

module.exports = {
    entry: './src/index.ts',
    output: {
        path: path.resolve(__dirname, 'build'),
        filename: 'main.bundle.js',
        library: 'cclexports'
    },
    mode: 'production',
    module: {
        rules: [
            {
                test: /\.ts$/,
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/preset-env', '@babel/preset-typescript'],
                    targets: { "node": 12 }
                }
            },
            {
                test: /\.js$/,
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/preset-env'],
                    targets: { "node": 12 }
                }
            }
        ]
    },
    resolve: {
        extensions: ['.ts', '.js', '.json'],
    },
    optimization: {
        minimize: false
    }
};
