(ns stardust.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<!]]
            [stardust.events :refer [frames keyboard]]
            [stardust.handlers :refer [handle]]
            [stardust.models :refer [game-screen]]
            [stardust.utils :as u]))

(enable-console-print!)

(defn events-loop
  []
  (let [events (a/merge [(frames) (keyboard)])]
    (go (loop [state (game-screen 1000 600)]
          (recur (handle state (<! events)))))))

(u/init-request-animation-frame)
(events-loop)
