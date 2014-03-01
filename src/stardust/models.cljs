(ns stardust.models)

(deftype CachedImage [offset-x offset-y data])

(defrecord Ship [x y vX vY thrust rotation rotate accelerate shoot ticks-before-shoot radius immunity type])

(defrecord GameScreen [width height ship])

(defn ship
  [x y immunity type]
  (Ship. x y 0 0 0 0 :none false false 0 30 immunity type))

(defn game-screen
  [width height]
  (GameScreen. width height (ship (/ width 2) (/ height 2) 100 0)))
