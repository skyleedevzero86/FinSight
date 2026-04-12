import { LIVE_META, LIVE_VIDEO_ID, VOD_SECTIONS } from "@/data/liveVodData"

export default function LiveVodClient() {
  return (
    <div className="finsight-live-vod-page">
      <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
        <div className="flv-toolbar">
          <h1>{LIVE_META.title}</h1>
        </div>

        <div className="flv-main-row">
          <div className="flv-video-col">
            <div className="flv-embed">
              <iframe
                title="finsight LIVE"
                src={`https://www.youtube.com/embed/${LIVE_VIDEO_ID}?autoplay=0&rel=0`}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowFullScreen
              />
            </div>
          </div>
        </div>

        {VOD_SECTIONS.map((sec) => (
          <section key={sec.heading} className="flv-relate">
            <h3>{sec.heading}</h3>
            <ul>
              {sec.items.map((it) => (
                <li key={it.href}>
                  <a href={it.href} target="_blank" rel="noopener noreferrer">
                    <img src={it.thumb} alt="" />
                    <div className="flv-vod-title">{it.title}</div>
                  </a>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}
