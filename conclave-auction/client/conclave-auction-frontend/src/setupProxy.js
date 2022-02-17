const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = app => {
  app.use(
    '/bidder1',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      pathRewrite: {'^/bidder1/auction-service' : '/auction-service'},
      changeOrigin: true,
        secure: false,
    })
  );
  app.use(
    '/bidder2',
    createProxyMiddleware({
      target: 'http://localhost:8082',
      pathRewrite: {'^/bidder2/auction-service' : '/auction-service'},
      changeOrigin: true,
        secure: false,
    })
  );
  app.use(
    '/bidder3',
    createProxyMiddleware({
      target: 'http://localhost:8083',
      pathRewrite: {'^/bidder3/auction-service' : '/auction-service'},
      changeOrigin: true,
        secure: false,
    })
  );
  app.use(
    '/admin',
    createProxyMiddleware({
      target: 'http://localhost:8084',
      pathRewrite: {'^/admin/auction-service' : '/auction-service'},
      changeOrigin: true,
        secure: false,
    })
  );
};