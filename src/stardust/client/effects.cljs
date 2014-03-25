(ns stardust.client.effects
  (:require [stardust.models :as m]
            [stardust.utils :as u]))

;;
;; Effects
;;

(defn create-hit-effect
  [{:keys [x y]}]
  (repeatedly (u/random-int 3 7) #(m/particle x y)))

(defn create-ship-explosion-effect
  [{:keys [x y vX vY h color] :as ship}]
  (let [points [[-10 10] [0 -15] [10 10] [7 5] [-7 5]]
        pieces (partition 2 1 (take 1 points) points)]
    (map #(m/ship-piece x y vX vY h color %) pieces)))
