(ns srcrepo.core
  (:import [java.security MessageDigest]
           [java.io FileInputStream File]))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def sha-256 (MessageDigest/getInstance "SHA-256"))


(defn file-name->ba [file-name]
  (let [in-stream (FileInputStream. file-name)
        ba (byte-array (.available in-stream))
        _ (.read in-stream ba)]
    ba))

(defn ba->ba-str [ba]
  (apply str (map (partial format "%02x") ba)))

(defn add-file-to-repo [src-file bin-files the-map]
   (let [k (ba->ba-str (.digest sha-256 (file-name->ba src-file)))]
     (contains? the-map k)
     (assoc the-map k (map file-name->ba bin-files))))

(defn files-of [dir ext]
  (let [ext (format ".%s" ext)]
    (filter (fn [path] (.endsWith (.getName path) ext)) (file-seq (clojure.java.io/file dir)))))

(defn relative-path-of [root-dir file]
  (.substring (.getAbsolutePath file) (inc (count (.getAbsolutePath root-dir)))))

(defn pair-src-file [src-dir bin-dir bin-files]
  (let [relative-bin-files (map (partial relative-path-of bin-dir) bin-files)]
    (fn [src-file] 
      (let [relative-src-file (relative-path-of src-dir src-file)
            relative-src-file-no-ext (.substring relative-src-file 0 (- (count relative-src-file) 5))]
        [(File. src-dir relative-src-file) 
         (map (fn [rf] (File. bin-dir rf)) (filter #(.startsWith % relative-src-file-no-ext) relative-bin-files))]
  ))))

(defn pair-src-bin-files [[src-dir src-ext] [bin-dir bin-ext]]
  (if (and (.isDirectory src-dir) (.isDirectory bin-dir))
   (let [src-files (files-of src-dir src-ext)
         bin-files (files-of bin-dir bin-ext)
         pair-src-file-fn (pair-src-file src-dir bin-dir bin-files)]
     (map pair-src-file-fn src-files))
  (throw (IllegalArgumentException. "Must be directory"))
  ))


(defn add-dir-to-repo [src bin the-map]
  (map (fn [[src-file bin-files]] (add-file-to-repo src-file bin-files the-map)) (pair-src-bin-files src bin))
  )


(comment 
  (pair-src-bin-files 
  (File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/src")
  (File. "/Users/anderseliasson/src/mz8/mz-main/mediationzone/packages/ultra/build/classes"))
  )
