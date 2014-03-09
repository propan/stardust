(ns stardust.client.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [chan put!]]
            [goog.events :as events]
            [stardust.utils :refer [round]]))

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)

(def KEY_DOWN_EVENTS
  {ARROW_LEFT  :arrow-left-down
   ARROW_UP    :arrow-up-down
   ARROW_RIGHT :arrow-right-down})

(def KEY_UP_EVENTS
  {ARROW_LEFT  :arrow-left-up
   ARROW_UP    :arrow-up-up
   ARROW_RIGHT :arrow-right-up})

(defn- keyboard-event
  [ch mapping e]
  (when-let [event (get mapping (.-keyCode e))]
    (put! ch [:keyboard event])))

(defn keyboard
  []
  (let [out (chan)]
    (events/listen js/document events/EventType.KEYDOWN
                   (fn [e] (keyboard-event out KEY_DOWN_EVENTS e)))
    (events/listen js/document events/EventType.KEYUP
                   (fn [e] (keyboard-event out KEY_UP_EVENTS e)))
    out))

(defn- animation-frame-loop
  [chan then time]
  (put! chan [:frame (round (/ 1000 (- time then)))])
  (.requestAnimationFrame js/window #(animation-frame-loop chan time %)))

(defn frames
  []
  (let [out (chan)
        now (.getTime (js/Date.))]
    (animation-frame-loop out now now)
    out))
