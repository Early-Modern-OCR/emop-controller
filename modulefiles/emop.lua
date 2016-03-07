-- Base modules
load("gcc/4.8.2")
load("python/2.7.10")
load("globusonline-python27-api-client/0.10.18")

-- Used for DeNoise
load("beautifulsoup4") --local
load("numpy/1.8.2") --local
load("scipy/0.14.0") --local

-- Used for Tesseract
load("leptonica/1.71")
load("icu/52.1")
load("tesseract/3.02-r889")

-- Used for PageCorrector
if (os.getenv("SLURM_JOB_ID")) then
  load("mariadb/10.0.15")
end

-- Used for PageCorrector, PageEvaluator, Juxta
load("java/1.7.0_67")

if (mode() == "load") then
  if (not os.getenv("EMOP_HOME")) then
    local cwd = lfs.currentdir()
    LmodMessage("WARNING: EMOP_HOME is not set, setting to ", cwd)
    setenv("EMOP_HOME", cwd)
  end
end

local emop_home = os.getenv("EMOP_HOME")
setenv("JUXTA_HOME", pathJoin(emop_home, "lib/juxta-cl"))
setenv("RETAS_HOME", pathJoin(emop_home, "lib/retas"))
setenv("SEASR_HOME", pathJoin(emop_home, "lib/seasr"))
setenv("DENOISE_HOME", pathJoin(emop_home, "lib/denoise"))
