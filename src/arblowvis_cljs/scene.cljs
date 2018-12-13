(ns arblowvis-cljs.scene
  (:require [arblowvis-cljs.audio :as aud]
            ))

(def th js/THREE)
(def th-x js/THREEx)
(def h-pi (/ js/Math.PI 2.0))
(def pic1-url "../res/bigcat.jpg")
(def pic2-url "../res/bigcat2.png")

(def debug? true)
(def txr-loader nil)
(def cube-mesh nil)
(def torus-mesh nil)
(def pic1-mesh nil)
(def pic2-mesh nil)
(def dir-light nil)


(def on-render-fns (atom []))
(def update-fns (atom []))
(def cur-opacity (atom 0))
(def ramp-val 0.003)
(def visible-thresh 0.1)

(def low-rms 0.1)
(def hi-rms 0.5)

(def min-rms (atom 0))
(def max-rms (atom 0)) ;; around 0.6?

(defn rms-scaler [input-val]
  (let [cur-range (- hi-rms low-rms)
        cur-scaler (/ 1.0 cur-range)
        cur-scaled (* (- input-val low-rms) cur-scaler)]
    (min 1.0 (max 0.0 cur-scaled))
    )
  )

(defn update-rms []
      ;;scale opacity here from rms given range of rms is 0 to 0.5
  (let [cur-rms (aud/get-rms)
        cur-val (rms-scaler cur-rms) ;; scaled val
        prev-opacity @cur-opacity]
    ;(when (> cur-rms @max-rms) (reset! max-rms cur-rms))
    ;(when (< cur-rms @min-rms) (reset! min-rms cur-rms))
    ;(.log js/console @min-rms @max-rms)
    (if (> cur-val prev-opacity)
      (swap! cur-opacity (fn [x] (/ (+ cur-val x) 2)))
      (swap! cur-opacity (fn [x] (max 0 (- x ramp-val))))
      )
    )
  ;(.log js/console @cur-opacity)
  )

(defn init-txr-loader []
  (set! txr-loader (th.TextureLoader.))
  )

(defn init-lights [cur-scene]
  (set! dir-light (th.DirectionalLight. 0xffffff 3.0))
  ;(.set (.-position dir-light) 0 3 3)
  (.add cur-scene dir-light)
  )

(defn init-cube [cur-root]
  (let [cur-geom (th.CubeGeometry. 0.5 0.5 0.5)
        cur-mat (th.MeshNormalMaterial.
                 (js-obj "transparent" true "opacity" 0.5 "side" th.DoubleSide))
        cur-mesh (th.Mesh. cur-geom cur-mat)
        geom-y (-> cur-geom .-parameters .-height)
        ]
    (set! cube-mesh cur-mesh)
    (set! (-> cube-mesh .-position .-y) (/ geom-y 2))
    (.add cur-root cube-mesh)

    ))

(defn init-torus [cur-root]
  (let [cur-geom (th.TorusKnotGeometry. 0.3 0.1 64 16)
        cur-mat (th.MeshNormalMaterial.)
        cur-mesh (th.Mesh. cur-geom cur-mat)
        ]
    (set! torus-mesh cur-mesh)
    (set! (-> torus-mesh .-position .-y) 0.5)
    (.add cur-root torus-mesh)
    (swap! update-fns conj
           (fn [] 
              (set! (-> torus-mesh .-rotation .-x)
                        (+ (-> torus-mesh .-rotation .-x) 0.1)))
           )
    )
  )

(defn init-pict [cur-root pic-url]
  ;;RETURNS mesh
  
  ;;width, height, widthsegments, heightsegments
  (let [cur-txr (.load txr-loader pic-url)
        ;cur-rto (/ 1024.0 768.0)
        cur-rto 1
        cur-width 1.5
        cur-height (* cur-width (/ 1 cur-rto))
        cur-geom (th.PlaneBufferGeometry. cur-width cur-height 1)
        cur-mat (th.MeshStandardMaterial. (js-obj
                                           "map" cur-txr
                                           "side" th.DoubleSide))
        geom-y (-> cur-geom .-parameters .-height)
        cur-plane (th.Mesh. cur-geom cur-mat)]
    (set! (.-anisotropy cur-txr) 8)
    (set! (.-flipY cur-txr) false)
    (set! (-> cur-plane .-position .-y) (/ geom-y 2))
    ;(set! (-> pic-mesh .-rotation .-z) 0.5)
    (set! (-> cur-plane .-rotation .-x) h-pi)

    (.add cur-root cur-plane)
    (swap! update-fns conj
           (fn []
             ;(.log js/console (-> pic-mesh .-rotation .-x))
             ;(set! (-> cur-plane .-rotation .-y)
             ;      (+ (-> cur-plane .-rotation .-y) 0.05))
             (if (true? debug?)
               (do (set! (-> cur-plane .-visible) true)
                   (set! (-> cur-plane .-material .-opacity) 1))
               (when (true? (.-visible cur-root))
                 (let [cur-op @cur-opacity]
                   (if (< cur-op visible-thresh) (set! (-> cur-plane .-visible) false)
                       (do
                         (set! (-> cur-plane .-visible) true)
                         (set! (-> cur-plane .-material .-opacity) @cur-opacity))
                       )
                   
                   )
                 )
               )
             )
           )
    cur-plane
    )
  )
        

(defn scene-init [cur-scene cur-render root1 root2]
  ;(init-cube cur-root)
  ;(init-torus cur-root)
  (init-txr-loader)
  (init-lights cur-scene)
  (set! pic1-mesh (init-pict root1 pic1-url))
  (set! pic2-mesh (init-pict root2 pic2-url))
  (swap! update-fns conj (fn [] (update-rms)))
  )
