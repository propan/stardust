(ns stardust.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [chan put!]]
            [goog.events :as events]))

(defn listen
  [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn- animation-frame-loop
  [chan then time]
  (put! chan :frame)
  (.requestAnimationFrame js/window #(animation-frame-loop chan time %)))

(defn frames
  []
  (let [out (chan)
        now (.getTime (js/Date.))]
    (animation-frame-loop out now now)
    out))
