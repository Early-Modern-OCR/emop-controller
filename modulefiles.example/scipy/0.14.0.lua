--[[

module load gcc/4.8.2 python/2.7.10 numpy/1.8.2 openblas
export BLAS=$BRAZOS_OPENBLAS_LIB/libopenblas.so
export LAPACK=$BLAS

LDFLAGS="-Wl,-rpath,$BRAZOS_OPENBLAS_LIB -shared" \
pip install --install-option="--prefix=/home/idhmc/apps/scipy/0.14.0" --install-option="--record=files.txt" --verbose scipy==0.14.0

module unload openblas
PYTHONPATH=/home/idhmc/apps/scipy/0.14.0/lib/python2.7/site-packages:$PYTHONPATH python -c 'import numpy; import scipy; scipy.test()'

]]

local prefix = "/home/idhmc/apps/scipy/0.14.0"
local pythonpath = pathJoin(prefix, "lib/python2.7/site-packages")

prereq(between("python", "2.7.0", "2.7.99"))

prepend_path("PYTHONPATH", pythonpath)
