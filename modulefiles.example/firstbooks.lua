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

-- Base modules
load("gcc/4.8.2")
load("python/2.7.10")
load("globusonline-python27-api-client/0.10.18")

-- Used for Ocular
load("java/1.7.0_67")
load("amd-opencl")
load("ocular")
