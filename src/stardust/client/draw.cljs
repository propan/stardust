(ns stardust.client.draw
  (:require-macros [stardust.client.macros :refer [with-context]])
  (:require [goog.string.format]
            [stardust.constants :as C]
            [stardust.client.constants :as CC]
            [stardust.models :refer [Bullet ObjectPiece Particle Player Ship ConnectionScreen DeathMatchScreen]]
            [stardust.utils :as u]))

;;
;; Helpers
;;

(defn- create-gradient
  [ctx x y radius color]
  (doto (.createRadialGradient ctx x y 0 x y radius)
    (.addColorStop 0 "#FFFFFF")
    (.addColorStop 0.4 "#FFFFFF")
    (.addColorStop 0.4 color)
    (.addColorStop 1.0 "#000000")))

(defn- fill-text
  [context text x y font color]
  (with-context [ctx context]
    (doto ctx
      (aset "font" font)
      (aset "fillStyle" color)
      (.fillText text x y))))

(defn- fill-text-centered
  [context text x y font color]
  (with-context [ctx context]
    (doto ctx
      (aset "font" font)
      (aset "fillStyle" color)
      (.fillText text (- x (/ (.-width (.measureText ctx text)) 2)) y))))

(defn- draw-cached-image
  [context image x y rotation]
  (with-context [ctx context]
    (doto ctx
      (.translate x y)
      (.rotate (* rotation C/RAD_FACTOR))
      (.drawImage (.-data image) (.-offset-x image) (.-offset-y image)))))

(defn- draw-path
  [context path]
  (when-let [[x y] (first path)]
    (.beginPath context)
    (.moveTo context x y)
    (doseq [[x y] (rest path)]
      (.lineTo context x y))
    (.closePath context)))

(defn- draw-shield
  [context immunity]
  (when (pos? immunity)
    (with-context [ctx context]
      (doto ctx
        (aset "strokeStyle" CC/SHIELD_COLOR)
        (aset "shadowBlur"  CC/SHADOW_BLUR)
        (aset "shadowColor" CC/SHIELD_COLOR)
        (aset "lineWidth"   1.5)
        (aset "globalAlpha" (/ immunity C/SPAWN_IMMUNITY_SECONDS))
        (.beginPath)
        (.arc 0 0 CC/SHIP_SHIELD_RADIUS (* 2 Math/PI) false)
        (.closePath)
        (.stroke)))))

(defn- fill-rect
  [context x y w h color alfa]
  (with-context [ctx context]
    (doto ctx
      (aset"fillStyle" color)
      (aset "globalAlpha" alfa)
      (.beginPath)
      (.rect x y w h)
      (.closePath)
      (.fill))))

(defn- stroke-rect
  [context x y w h color]
  (with-context [ctx context]
    (doto ctx
      (aset"strokeStyle" color)
      (.beginPath)
      (.rect x y w h)
      (.closePath)
      (.stroke))))

(defn- draw-life-bar
  [context life]
  (with-context [ctx context]
    (let [health (/ life C/MAX_PLAYER_LIFE)
          color  (cond
                  (< health 0.33) CC/LIFE_DANGER_COLOR
                  (< health 0.66) CC/LIFE_WARN_COLOR
                  :else           CC/LIFE_OK_COLOR)
          status (str (u/round (/ health 0.01)) "%")]
      (doto ctx
        (.translate 15 30)
        (stroke-rect 0 -15 150 15 color)
        (fill-rect 0 -15 (* 150 health) 14 color 0.25)
        (fill-text status 60 -4 "10px Helvetica" "#FFFFFF")))))

(defn- draw-score-board
  [context player ships score]
  (with-context [ctx context]
    (.translate ctx (- CC/SCREEN_WIDTH 55) 5)
    (loop [score (sort-by second > score)
           index 0]
      (when-let [[cid points] (first score)]
        (let [color (or (get-in ships [cid :color])
                        (:color player))
              color (get CC/SHIP_COLORS color)]
          (fill-rect ctx 10 (+ 5 (* index 15)) 11 11 color 1)
          (fill-text ctx (goog.string.format "%3d" points) 30 (+ 15 (* index 15)) "14px Helvetica" "#FFFFFF")
          (recur (rest score) (inc index)))))))

;;
;; Pre-generated images
;;

(deftype CachedImage [offset-x offset-y data])

(defn- generate-ship-image
  [buffer color]
  (let [image-width  (+ 20 (* 2 CC/SHADOW_BLUR))
        image-height (+ 25 (* 2 CC/SHADOW_BLUR))
        middle-x     (/ image-width 2)
        middle-y     (/ image-width 2)
        image        (js/Image.)]
    (set! (.-width buffer) image-width)
    (set! (.-height buffer) image-height)
    (with-context [ctx (.getContext buffer "2d")]
      (doto ctx
        (aset "shadowBlur" CC/SHADOW_BLUR)
        (aset "shadowColor" color)
        (aset "strokeStyle" color)
        (aset "lineWidth" 2.5)
        (.translate middle-x middle-y)
        (.beginPath)
        (.moveTo -10 10)
        (.lineTo 0 -15)
        (.lineTo 10 10)
        (.moveTo 7 5)
        (.lineTo -7 5)
        (.closePath)
        (.stroke)))
    (set! (.-src image) (.toDataURL buffer "image/png"))
    (CachedImage. (- middle-x) (- middle-y) image)))

(defn- generate-particle-image
  [buffer size color]
  (let [x-offset     size
        y-offset     (+ 3 size)
        middle-x     (+ x-offset CC/SHADOW_BLUR)
        middle-y     (+ y-offset CC/SHADOW_BLUR)
        image-width  (* 2 middle-x)
        image-height (* 2 middle-y)
        image        (js/Image.)]
    (set! (.-width buffer) image-width)
    (set! (.-height buffer) image-height)
    (with-context [ctx (.getContext buffer "2d")]
      (doto ctx
        (aset "shadowBlur" CC/SHADOW_BLUR)
        (aset "shadowColor" color)
        (aset "fillStyle" color)
        (.translate middle-x middle-y)
        (draw-path [[0 y-offset] [x-offset 0] [0 (- y-offset)] [(- x-offset) 0]])
        (.fill)))
    (set! (.-src image) (.toDataURL buffer "image/png"))
    (CachedImage. (- middle-x) (- middle-y) image)))

;;
;; Cached images
;;

(def ship-images
  (let [buffer (.createElement js/document "canvas")]
    (into-array (for [color (range 0 6)]
                  (generate-ship-image buffer (get CC/SHIP_COLORS color))))))

(def particle-images
  (let [buffer (.createElement js/document "canvas")]
    (into-array (for [size (range 1 3)]
                  (generate-particle-image buffer size CC/PARTICLE_COLOR)))))

(def bullet-image
  (generate-particle-image (.createElement js/document "canvas") 2 CC/BULLET_COLOR))
;;
;;
;;

(defn- draw-ship
  [context x y rotation immunity color]
  (with-context [ctx context]
    (doto ctx
      (draw-cached-image (aget ship-images color) x y rotation)
      (.translate x y)
      (draw-shield immunity))))

;;
;; Drawable Protocol
;;

(defprotocol Drawable
  (draw [_ context]))

(extend-type Bullet
  Drawable
  (draw [{:keys [x y h]} context]
    (draw-cached-image context bullet-image x y h)))

(extend-type ObjectPiece
  Drawable
  (draw [{:keys [x y h size rotation color path]} context]
    (with-context [ctx context]
      (let [scale (* 1.5 size)
            width (* 1.0 (/ 1.5 size))
            color (get CC/SHIP_COLORS color)]
        (doto ctx
          (aset "shadowBlur" 15)
          (aset "shadowColor" color)
          (aset "lineWidth" width)
          (aset "strokeStyle" color)
          (.translate x y)
          (.scale scale scale)
          (.rotate (* rotation C/RAD_FACTOR))
          (draw-path path)
          (.stroke))))))

(extend-type Particle
  Drawable
  (draw [{:keys [x y h s]} context]
    (draw-cached-image context (aget particle-images (dec s)) x y h)))

(extend-type Ship
  Drawable
  (draw [{:keys [x y h immunity color]} context]
    (draw-ship context x y h immunity color)))

(extend-type Player
  Drawable
  (draw [{:keys [x y h immunity color life]} context]
    (doto context
      (draw-ship x y h immunity color)
      (draw-life-bar life))))

(defn connecting-string
  []
  (let [times (mod (.getSeconds (js/Date.)) 4)]
    (apply str (cons "Connecting" (repeat times ".")))))

(extend-type ConnectionScreen
  Drawable
  (draw [{:keys [fps] :as state} context]
    (doto context
      (.clearRect 0 0 CC/SCREEN_WIDTH CC/SCREEN_HEIGHT)
      (fill-text (connecting-string) (- (/ CC/SCREEN_WIDTH 2) 110) (/ CC/SCREEN_HEIGHT 2) "34px Rammetto One" "#FFFFFF"))))

(extend-type DeathMatchScreen
  Drawable
  (draw [{:keys [player fps ships effects bullets score] :as state} context]
    (.clearRect context 0 0 CC/SCREEN_WIDTH CC/SCREEN_HEIGHT)
    (fill-text context (str fps " FPS") (- CC/SCREEN_WIDTH 80) (- CC/SCREEN_HEIGHT 15) "14px Helvetica" "#FFFFFF")
    (doseq [bullet bullets]
      (draw bullet context))
    (draw player context)
    (doseq [[cid ship] ships]
      (draw ship context))
    (doseq [effect effects]
      (draw effect context))
    (draw-score-board context player ships score)))
