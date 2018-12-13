(ns arblowvis-cljs.thx
  (:require [arblowvis-cljs.scene :as scn]
            ))

(def win js/window)
(def doc js/document)
(def docbody (.-body doc))
(def baseurl "three.js/")
(def hirourl "/../../data/patt.hiro")
(def kwanurl "/../../data/pattern-marker-kwan.patt")

(def th js/THREE)
(def th-x js/THREEx)
(def renderer nil)
(def update-fns (atom []))
(def on-render-fns (atom []))

(def art-src nil)
(def art-ctx nil)

(def hiro-root nil)
(def kwan-root nil)

(def scene nil)
(def camera nil)

(def last-ms (atom 0))



(defn init-renderer []
  (.log js/console "init-renderer")
  (let [cur-render (th.WebGLRenderer. (js-obj "alpha" true))]
    (set! renderer cur-render)
    (.setClearColor renderer (th.Color. "lightgrey") 0)
    (.setSize renderer (.-innerWidth win) (.-innerHeight win))
    (set! (-> renderer .-domElement .-style .-position) "absolute")
    (set! (-> renderer .-domElement .-style .-top) "0px")
    (set! (-> renderer .-domElement .-style .-left) "0px")
    (.appendChild docbody (.-domElement renderer))

    )
  )

(defn init-three []
  (.log js/console "init-three")
  (let [cur-scene (th.Scene.)
        cur-cam (th.Camera.)]
    (set! scene cur-scene)
    (set! camera cur-cam)
    (.add scene camera)

    )
  )
    
  

(defn on-resize []
  (.onResize art-src)
  (.copySizeTo art-src (.-domElement renderer))
  (when (not (nil? (.-arController art-ctx)))
    (.copySizeTo art-src (-> art-ctx .-arController .-canvas))
    )
  )

(defn artsrc-init []
  (.log js/console "artoolkit-source init")
  (let [cur-params (js-obj "sourceType" "webcam")
        ;cur-params (js-obj "sourceType" "image" "sourceUrl" "../res/testimg.jpg")
        cur-ar (th-x.ArToolkitSource. cur-params)]
    (set! art-src cur-ar)
    (.init art-src #(on-resize))

    ;;on-ready, on-error
    (.addEventListener win "resize" #(on-resize))
    )
  )

(defn artctx-init []
  (.log js/console "artoolkit-context init")
  (let [param-url "../data/camera_para.dat"
        cur-params (js-obj
                    "cameraParametersUrl" param-url
                    "detectionMode" "mono"
                    "maxDetectionRate" 30
                    "canvasWidth" (* 80 3)
                    "canvasHeight" (* 60 3))
        cur-ctx (th-x.ArToolkitContext. cur-params)]
    (set! art-ctx cur-ctx)
    ;; on-completed
    (.init art-ctx
           (fn []
             (.copy (.-projectionMatrix camera) (.getProjectionMatrix art-ctx)))
           )
    ;;(.log js/console (.-ready art-ctx))

    (swap! on-render-fns conj
           (fn [] (when (true? (.-ready art-src))
                    ;(.log js/console "artoolkit-ctx ready")
                    (.update art-ctx (.-domElement art-src))
                    )
              ))
    )
  
  )


(defn marker-init [patt-url]
  ;; RETURNS smoothed-root
  
  (let [cur-marker-root (th.Group.)
        marker-params (js-obj "type" "pattern" "patternUrl" (str baseurl patt-url))
        cur-marker-ctl (th-x.ArMarkerControls. art-ctx cur-marker-root marker-params) ;; binds controls to root somehow?
        cur-smooth-root (th.Group.)
        smooth-params (js-obj "lerpPosition" 0.4 "lerpQuaternion" 0.3 "lerpScale" 1)
        cur-smooth-ctl (th-x.ArSmoothedControls. cur-smooth-root smooth-params) ;; binds controls to root somehow?
        ]
    (.add scene cur-smooth-root)
    (swap! on-render-fns conj (fn [cur-delta] (.update cur-smooth-ctl cur-marker-root)))
    cur-smooth-root

    )
  )

(defn update-all [delta-ms cur-ms]
  ;(.log js/console "updating")
  (let [cur-update-fns @update-fns
        all-to-update (into cur-update-fns @scn/update-fns)
        ]
    (doall (map (fn [cur-fn] (cur-fn delta-ms cur-ms)) all-to-update))
    )
  )

(defn render-all [delta-ms cur-ms]
  ;(.log js/console "rendering")
  (let [render-fns @on-render-fns
        all-to-render (into render-fns @scn/on-render-fns)
        ]
    (doall (map (fn [cur-fn] (cur-fn delta-ms cur-ms)) all-to-render))
    (.render renderer scene camera)
    )
  )



(defn loop-init []
  (js/requestAnimationFrame
   (fn animate [cur-ms]
     (js/requestAnimationFrame animate)
     (swap! last-ms (fn [x] (- x (/ 1000.0 60.0))))
     (let [delta-ms (min 200 (- cur-ms @last-ms))]
       (reset! last-ms cur-ms)
       (update-all (/ delta-ms 1000.0) (/ cur-ms 1000.0))
       (render-all (/ delta-ms 1000.0) (/ cur-ms 1000.0))
       )
     )
  )
  )

;;(defn loop-init []
;;  (let [last-ms (atom 0)]
;;      (.setAnimationLoop renderer
;;                         (fn [cur-ms]
;;                           (swap! last-ms (fn [x] (- x (/ 1000 60))))
;;                           (let [delta-ms (min 200 (- cur-ms @last-ms))]
;;                             (swap! last-ms cur-ms)
;;                             (update-all (/ delta-ms 1000) (/ cur-ms 1000))
;;                             (render-all (/ delta-ms 1000) (/ cur-ms 1000))
;;                             ))
;;                         )
;;      )
;;  )

(defn init []
  (init-renderer)
  (init-three)
  (artsrc-init)
  (artctx-init)
  (set! kwan-root (marker-init kwanurl))
  (set! hiro-root (marker-init hirourl))

  ;(smooth-init)
  (scn/scene-init  scene renderer hiro-root kwan-root)
  (loop-init)
  ;(.log js/console (.-children smooth-root))
  )
  
