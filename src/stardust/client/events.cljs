(ns stardust.client.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [chan put! <!]]
            [cljs.reader :as reader]
            [goog.events :as events]
            [goog.net.WebSocket]
            [goog.net.WebSocket.EventType :as TYPE]
            [stardust.utils :refer [round]]))

(cljs.reader/register-tag-parser!  "stardust.models.DeathMatchScreen" stardust.models/map->DeathMatchScreen)
(cljs.reader/register-tag-parser!  "stardust.models.Bullet" stardust.models/map->Bullet)
(cljs.reader/register-tag-parser!  "stardust.models.ObjectPiece" stardust.models/map->ObjectPiece)
(cljs.reader/register-tag-parser!  "stardust.models.Particle" stardust.models/map->Particle)
(cljs.reader/register-tag-parser!  "stardust.models.Player" stardust.models/map->Player)
(cljs.reader/register-tag-parser!  "stardust.models.Ship" stardust.models/map->Ship)

(def SPACE 32)

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)

(def KEY_DOWN_EVENTS
  {ARROW_LEFT  :arrow-left-down
   ARROW_UP    :arrow-up-down
   ARROW_RIGHT :arrow-right-down
   SPACE       :space-down})

(def KEY_UP_EVENTS
  {ARROW_LEFT  :arrow-left-up
   ARROW_UP    :arrow-up-up
   ARROW_RIGHT :arrow-right-up
   SPACE       :space-up})

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
  (put! chan [:frame (- time then)])
  (.requestAnimationFrame js/window #(animation-frame-loop chan time %)))

(defn frames
  []
  (let [out (chan)
        now (.getTime (js/Date.))]
    (animation-frame-loop out now now)
    out))

(defn- safe-read-string
  [str]
  (try
    (reader/read-string str)
    (catch :default e nil)))

(defn websocket
  [url]
  (let [ws  (goog.net.WebSocket.)
        in  (chan)
        out (chan)]
    (events/listen ws TYPE/OPENED  (fn [e]
                                     (put! in [:socket [:opened e]])))
    (events/listen ws TYPE/CLOSED  (fn [e]
                                     (put! in [:socket [:closed e]])))
    (events/listen ws TYPE/MESSAGE (fn [e]
                                     (when-let [event (safe-read-string (.-message e))]
                                       (put! in [:socket [:message event]]))))
    (events/listen ws TYPE/ERROR   (fn [e]
                                     (put! in [:socket [:error e]])))
    (.open ws url)
    (go (loop [msg (<! out)]
          (when msg
            (.send ws msg)
            (recur (<! out)))))
    {:in in :out out}))
