(ns stardust.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<!]]
            [clojure.browser.dom :as dom]
            [stardust.events :refer [frames keyboard]]
            [stardust.handlers :as h]
            [stardust.models :as m]
            [stardust.utils :as u]))

(enable-console-print!)

(def canvas (dom/get-element "open-space"))
(def ctx (.getContext canvas "2d"))

(defn events-loop
  []
  (let [events (a/merge [(frames) (keyboard)])]
    (go (loop [state (m/game-screen)]
          (recur (h/handle state (<! events)))))))

(u/init-request-animation-frame)
(events-loop)
