from os import listdir, path

APACHE = """/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"""

srcDirs = {
  "common/src": APACHE,
  "fabric/src": APACHE,
  "forge/src": APACHE
}

def IterPaste(fold, licenseText):
    for fileName in listdir(fold):
        filePath = path.join(fold, fileName)

        if path.isfile(filePath) and filePath.endswith(".java"):
            print("Modifying " + filePath)
            with open(filePath) as fil:
                rawtxt = fil.read()
                index = rawtxt.find("package")

                if (index == -1):
                    print("Could not find package declaration. Skipping!")
                    continue

                txt = licenseText + rawtxt[index:]
            with open(filePath, "w") as fil:
                fil.write(txt)
        elif path.isdir(filePath):
            IterPaste(filePath, license)

for srcDir, license in srcDirs.items():
    IterPaste(srcDir, license)
