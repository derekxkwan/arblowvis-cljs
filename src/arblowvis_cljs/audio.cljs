(ns arblowvis-cljs.audio)

(def win js/window)
(def nav-media (.-mediaDevices js/navigator))
(def fftsize 1024)
(def snd-data nil)
(def datalen 0)
(def ctx nil)
(def mic-input nil)
(def analyzer nil)


(defn get-rms []
  (if (not (nil? mic-input))
    (do
      (.getFloatTimeDomainData analyzer snd-data)
      ;; so ugly, figure out how to use normal cljs map/apply?
      (let [sq-sum (.reduce (.map snd-data (fn [z] (js/Math.pow z 2)))
                            (fn [x y] (+ x y)))]
        (js/Math.pow (/ sq-sum datalen) 0.5)
        )
      )
    0
    )
  )
  

(defn init-analyzer [cur-ctx]
  (let [cur-analyzer (.createAnalyser cur-ctx)
        cur-buflen (.-frequencyBinCount cur-analyzer)
        cur-array (js/Float32Array. cur-buflen)]
    (set! datalen cur-buflen)
    (set! snd-data cur-array)
    (set! (.-fftSize cur-analyzer) fftsize)
    cur-analyzer
  )
  )
  

(defn init-mic [cur-ctx cur-stream cur-analyzer]
  (let [cur-src (.createMediaStreamSource cur-ctx cur-stream)]
    (.connect cur-src cur-analyzer)
    cur-src
    )
  )

(defn init []
  (set! ctx (if (.-AudioContext win) (win.AudioContext.) (win.webkitAudioContext.)))
  (when (not (nil? nav-media))
    (let [usr-med (.getUserMedia nav-media (js-obj "audio" true))]
      (.then usr-med (fn [stream]
                       (let [cur-analyzer (init-analyzer ctx)
                             cur-src (init-mic ctx stream cur-analyzer)]
                         (set! analyzer cur-analyzer)
                         (set! mic-input cur-src)
                         )
                       ))
      )
    )
  )
  
