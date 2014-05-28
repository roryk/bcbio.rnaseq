(ns bcbio.rnaseq.stats
  (:require
   [incanter.stats :as stats]
   [clojure.math.numeric-tower :as math]))

(defn rescale [xs new-min new-max]
  "rescale xs so that the range is from new-min to new-max"
  (let [old-min (apply min xs)
        old-max (apply max xs)]
    (map #(+ (* (/ (- % old-min) (- old-max old-min)) (- new-max new-min))
             new-min) xs)))

(defn iqr [xs]
  "calculate interquartile range of xs"
  (- (stats/quantile xs :probs 0.75) (stats/quantile xs :probs 0.25)))

(defn- bw-nrd0 [xs]
  "calculate the nrd0 bandwidth of xs"
  (let [lo (min (stats/sd xs) (/ (iqr xs) 1.34))]
    (* 0.9 lo (math/expt (count xs) -0.2))))

(defn- gaussian-weight [t]
  (let [k (Math/pow (* 2 Math/PI) -0.5)]
    (* k (Math/exp (/ (* t t) -2)))))

(defn- kernel-weight-function [h]
  (defn weight-function [t]
    (* (/ 1 h) (gaussian-weight (/ t h)))))

(defn gaussian-kernel [xs]
  (let [bw (bw-nrd0 xs)
        n (count xs)
        weight-function (kernel-weight-function bw)]
    (defn kernel [x]
      (let [ts (map #(- x %) xs)]
        (* (/ 1 n ) (apply + (map weight-function ts)))))))
