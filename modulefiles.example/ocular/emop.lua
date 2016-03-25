--[[

cd /dev/shm
git clone https://github.com/tberg12/ocular.git
cd ocular

module load java/1.7.0_79
export _JAVA_OPTIONS=-Xmx512M
./make_jar.sh

mkdir /home/idhmc/apps/ocular
mkdir /home/idhmc/apps/ocular/0.3-snapshot
cp -a conf /home/idhmc/apps/ocular/0.3-snapshot/
cp -a ocular-0.3-SNAPSHOT-with_dependencies.jar /home/idhmc/apps/ocular/0.3-snapshot/ocular.jar
cp -a LICENSE.txt README.md README.txt /home/idhmc/apps/ocular/0.3-snapshot/

]]

local emop_home = os.getenv("EMOP_HOME")
local prefix = pathJoin(emop_home, "lib/ocular")

prereq("java")

setenv("OCULAR_ROOT", prefix)
setenv("OCULAR", pathJoin(prefix, "ocular.jar"))
