const YOUTUBE_ORIGINS =
  '(self "https://www.youtube-nocookie.com" "https://www.youtube.com")'

const LIVE_VOD_PERMISSIONS_POLICY = [
  `compute-pressure=${YOUTUBE_ORIGINS}`,
  `accelerometer=${YOUTUBE_ORIGINS}`,
  `autoplay=${YOUTUBE_ORIGINS}`,
  `encrypted-media=${YOUTUBE_ORIGINS}`,
  `gyroscope=${YOUTUBE_ORIGINS}`,
  `picture-in-picture=${YOUTUBE_ORIGINS}`,
  `fullscreen=${YOUTUBE_ORIGINS}`,
  `clipboard-write=${YOUTUBE_ORIGINS}`,
].join(", ")

const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "ext.same-assets.com",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "images.unsplash.com",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "i.ytimg.com",
        pathname: "/**",
      },
    ],
  },
  async headers() {
    return [
      {
        source: "/live-vod",
        headers: [
          {
            key: "Permissions-Policy",
            value: LIVE_VOD_PERMISSIONS_POLICY,
          },
        ],
      },
      {
        source: "/live-vod/:path*",
        headers: [
          {
            key: "Permissions-Policy",
            value: LIVE_VOD_PERMISSIONS_POLICY,
          },
        ],
      },
    ]
  },
}

module.exports = nextConfig
