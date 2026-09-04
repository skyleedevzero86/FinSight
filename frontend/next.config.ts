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
  // 개발 중 HMR/컴파일이 길면 chunk 로드가 기본 타임아웃에 걸릴 수 있음
  webpack: (config: { output?: Record<string, unknown> }, { dev, isServer }: { dev: boolean; isServer: boolean }) => {
    if (dev && !isServer) {
      config.output = {
        ...config.output,
        chunkLoadTimeout: 300_000,
      }
    }
    return config
  },
  async redirects() {
    return [
      { source: "/my", destination: "/myinfo", permanent: true },
      { source: "/my/:path*", destination: "/myinfo/:path*", permanent: true },
    ]
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
