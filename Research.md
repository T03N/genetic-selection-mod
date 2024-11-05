# Real World Natural selection
## Hardy-Weinberg frequency 
* https://en.wikipedia.org/wiki/Hardy%E2%80%93Weinberg_principle

The principle that states that in the absence of external selection factors, allele and genotype frequencies will remain constant over a large population size
 
* Let p and q be the frequency of an allele in a given population
* p2 + 2pq + q2 = 1

If p2 + 2pq + q2 does NOT equal to 1, evolution is occuring, and there are selection pressures happening. ie;
predators are evolving to be stronger, food scarcity, etc. The above equation can be used to “measure” evolution in a population and as a potential debugging tool if telemetry on allele frequency can be implemented.

## Genetic drift
* https://en.wikipedia.org/wiki/Genetic_drift 

Genetic drift is the change in the frequency of alleles due to chance. This Affects smaller populations the most.

## Effective population size
* https://www.uwyo.edu/dbmcd/molmark/lect07/lect7.html 

The number of a species needed to roughly match the progress of genetic drift as the wider population.

## Heritability
* https://www.britannica.com/science/heritability

The amount of observational variation found in a population that can be chalked up to genetics and not environmental differences.

* Let Vg be the variation in genotype
* Let Vp be the variation in phenotype
* Let H be the heritability estimate
* H2 = Vg / Vp

H will range from 0 to 1. If H = 0, there is no genetic variation. If H = 1, all variation is due to genetics.

## Selection differential 
* https://excellenceinbreeding.org/sites/default/files/manual/EiB_M2_Selection%20Intensity_26-10-20.pdf 

The Selection Differential is the difference between the mean trait value of the selected parents and the overall population mean.

## Response to selection
* https://www.ndsu.edu/pubweb/~mcclean/plsc431/quantgen/qgen7.htm#:~:text=The%20selection%20response%20is%20how,heriability%20by%20the%20selection%20differntial. 

Response to Selection is the "gain" made when mating selected parents. It is found by multiplying heritability by selection differential.

## Selection intensity
* https://breedplan.une.edu.au/general/a-breedplan-guide-to-genetic-improvement/#:~:text=Selection%20Intensity%20(i),(see%20Figure%201%20below). 

The Selection Intensity is the difference in the average genetic value of the animals selected for breeding versus the average genetic value of all animals in the population from which they were selected.
## Inbreeding vs Outbreeding
### Inbreeding
Inbreeding makes the offspring susceptible to recessive genetic disease from increasing the chance that those who alleles can come together. Inbreeding results in lower genetic diversity, making populations vunerable to all the same things. An example would be the Cavendish Bananas, which are all effectively identical in terms of genetics. As a result, a virus that can kill one has the potential to make the breed extinct.

### Outbreeding
Outbreeding creates genetically diverse populations, which is ideal for fitness in a species. If done too far out, and two species with low genetic compatibility breed, the offspring could have reduced fitness. An example of this would be the Mule, a horse and donkey hybrid that cannot reproduce, which does not allow for it to pass of genes to offspring.

# Basics of behavior and breeding of Minecraft animals:
## General
To breed two animals of the same species in Minecraft, both must be in "love mode," which is indicated by hearts appearing around them. Once in love mode, the animals will find a path toward each other, and an offspring will spawn. To enter love mode, animals need to be fed specific foods that vary by species. Some animals also require taming before they can breed. An animal will exit love mode after 30 seconds if it does not breed, and once they have bred, the parents cannot breed again for 5 minutes. 

This information is for the Java edition; there may be differences between the Bedrock edition and Minecraft Education. As of Minecraft version 1.21.1, some of the animals with the most interesting behaviors and breeding properties include horses, pandas, turtles, and foxes.

## Horse
To breed horses they first need to be tamed. This is done by repeatedly getting on the horse's back until the horse relaxes and lets the player stay on its back. To enter love mode, use a golden apple, enchanted golden apple, or golden carrot. 

Horses have health ranging from 15 to 30 hearts, where one heart equals 2 health points. Horses exhibit jump strength from 0.4 to 1.0, allowing them to jump approximately 1.11 to 5.3 blocks. Players are able to test this by jumping over walls. Their speed varies between 0.1125 to 0.3375 blocks per tick, equating to 4.74 blocks/sec to 42.16 blocks/sec. Players must use repeaters to measure this.

Specific to horses, breeding averages the traits of the two parent horses with an addition of random variance. The maximum value for strength and jump, to make the "perfect" horse is impossible to achieve through breeding. Foals inherit the color and markings of one parent, with a 13/45 chance of having a random appearance.

## Panda
Pandas can have one of 6 different personalities. Normal pandas do not have unique personality behaviors but are identifiable by their frown. Lazy pandas tend to lie on their back and are slower than normal pandas. They are also physically identifiable by their smile. Worried pandas have a worried expression on their face and they typically avoid players and hostile mobs. During thunderstorms, they are also known to hide or shake their faces. Playful pandas have a tongue that sticks out, they also roll over and jump around (usually only done by panda cubs). Aggressive pandas are identifiable by their thick eyebrows and frowns. When hit, the panda will attack the mob or player until the target dies or is no longer in the detection range. They also become aggressive towards mobs/players that hit nearby pandas. Weak pandas have teary eyes and snotty noses, and they tend to sneeze more often. Pandas have a 1/6000 chance of sneezing every tick, with weak baby pandas sneezing more frequently (1/500). Sneezing causes all adult pandas within 10 blocks to jump and may drop a slimeball. Baby pandas occasionally roll over and jump around. Brown pandas have no identifiable characteristics besides their brown fur and frown. 

For breeding, pandas need to be fed bamboo and there also must be at least one bamboo block within a 5-block radius. If there’s no bamboo block, the panda will shake their heads and will not enter love mode. Pandas have genetics and have two hidden values: a main gene and a hidden gene. Each gene corresponds to a particular personality type which is either dominant or recessive. Dominant personality types: normal, aggressive, lazy, worried, and playful. Recessive personality types: weak and brown. If the main gene is dominant, the main personality will show. If the main gene is recessive and the hidden gene is a different trait, a normal personality will show. The recessive personality type will only show if both the main and hidden genes are the same. The offspring's main and hidden genes are randomly chosen from the parent's genes. There is also a 1/32 chance for the genes to mutate. 

## Turtle
When born, turtles remember which block they were hatched in as their "home beach". Throughout its life, it will try to return to this beach. 

They require seagrass to enter love mode. Unlike other animals that will spawn offspring, turtles lay eggs. One of the turtles will gain an egg in their inventory, this will reflect physically as well. It will then return back to its home beach and lay the egg up to 9 blocks from its spawn block. The egg can only be hatched on the sand, red sand, or suspicious sand and takes about 4-5 in-game days.

## Fox
Foxes can carry items in their mouths (shows up physically in their mouths). If a fox is holding a totem of undying, it can revive itself if it dies. Similarly, if a fox picks up an enchanted item, it will inherit those enchants. These items are usually found on the ground, with picking up food items preferred. 

Another behavior foxes have are attacking other mobs. They will attack chickens, rabbits, cod, salmon, tropical fish, and turtles. They will also attack wolves if they are attacked by them first. Interestingly, red foxes prefer to attack chickens, rabbits, and baby turtles. Baby foxes typically follow their parent however they are not able to swim. So if the parent goes to a body of water, the baby fox can drown. 

For breeding, use sweet berries or glow berries. Breeding a red fox with a white fox will result in either a red fox or a snow fox with a 50% chance for either. The offspring will trust the player and can be tamed by leading it away from the parent foxes. 

## Donkey
Donkeys have set attributes that are the same for all donkeys. Their speed is set to 0.175 (about 7.38 blocks/sec) and their jump strength is 0.5 (1 and 9/16 blocks). Their health can range from 15-30 hearts.  

Similar to horses, the player must tame the donkey by repeatedly getting on its back. Once tamed, donkeys can be bred by entering love mode using golden apples, enchanted golden apples, and golden carrots. The offspring will spawn as untamed and can only be tamed when they are adults. 

## Mule
Mules do not spawn naturally but they can be bred using a horse and a donkey. Players can not breed two mules together. If spawned using commands or a spawn egg, the mule will have assigned stats according to their horse type. If created by breeding, the mule will have stats that are the average of parent donkey and parent horse. All mules have a speed of 0.175 and a jump strength of 0.5. Just like its parents, players can ride mules.

## Llama
All llamas have a set number of 15 hearts and half a heart of-attack strength. They cannot be ridden by players, but there are trader llamas. They are bred using hay bales. A baby llama takes a coat of one parent at random. Strength is randomly chosen between 1 and the strength of the stronger parent, with a 3% chance of increasing strength by 1 (capped at 5). Taming is easier with a higher temper value, which ranges from 0 to 99.

## Bee
Bee nests generate three bees. They hover a few blocks above the ground rather than flying. They can "fly" horizontally and vertically upward through scaffolding but do can not "fly" downward. Bees are attracted to flowers and can pollinate wither roses, although they die as a result. When a bee circles a flower or berry bush for 30 seconds, it collects pollen, which changes the texture of its abdomen to include pollen spots. Pollinated plants grow faster. Bees return to their hives to produce honey and go back to their nests at night. Homeless bees wander in search of empty beehives. 

Bees nearby become angry if one is attacked or if the beehive is destroyed. A bee loses its stinger after attacking a player and dies one minute later. To breed bees, use flowers, flowering azalea, cherry leaves, flowering azalea leaves, or mangrove propagule. It is possible to breed at least one angered bee with another.

## Frog
Frogs naturally spawn in swamps and mangrove swamps and can be spawned by growing from tadpoles. There are three frog variants based on the biome where the tadpole grew up: temperate, warm, and cold. Differences can be seen physically through their color. Frogs can jump up to 8 blocks high and take less damage from falling than other mobs. They prefer jumping on lily pads and big drip leaves, swim upwards, and can croak while inflating their vocal sac.

Breeding requires slimeballs.  One frog will become pregnant and search for a water block that has at least one adjacent water block with air above. Once this condition is met, they will lay frogspawn, which eventually hatches into tadpoles.

## Rabbit
Different biomes spawn rabbits of various colors. Wild wolves, foxes, and stray cats tend to kill/attack rabbits. Breed rabbits using carrots, golden carrots, or dandelions. Offspring usually inherit the same fur type as one of the parents (47.5% chance per parent), with a 5% chance of matching the current biome. Killer bunnies can breed with other rabbits and may produce another killer bunny, which is hostile to players, foxes, and wolves. Killer bunnies have pure white fur and blood-red eyes and can only be spawned using commands. Toast, a special rabbit, can be created with a name tag.

## Camel
Players can ride camels using a saddle. They can walk slowly, sprint, and perform a special "dashing charge" when ridden. They are tall enough that most mobs can not reach the player riding it. Camels heal when fed cacti. It is also used to breed 2 adult camels. 

## Axolotl
Axolotls are bred using buckets of tropical fish and inherit the color of one parent at random. There are five possible colors: pink, brown, gold, cyan, and blue. There is a 1/2000 chance of a baby spawning with a blue variant.

Axolotls are passive toward players and can be attached to leads. In combat, each axolotl provides Regeneration I for 5 seconds per axolotl, up to 2 minutes, and removes Mining Fatigue when a player defeats a mob engaged with an axolotl. They will attack all other aquatic mobs except for turtles, dolphins, and frogs. They deal 2 damage per attack. If they take damage while underwater, there is a 1/3 chance that they play dead for 10 seconds and gain Regeneration I. This chance is higher if they suffer a significant amount of damage or if they are less than 50% healthy. They are also able to go on land, as long as they are in a 16-block radius of deep water. They die after 5 minutes of not being in the water.

## Mooshroom
Mooshrooms are a special variant of cows found in the Mushroom Fields biome. They come in two colors: red and brown. The red mooshroom can transform into a brown mooshroom if struck by lightning. To breed mooshrooms, the player needs to feed them wheat.

Unlike regular cows, mooshrooms can’t breed with normal cows unless they have been sheared first. When two red mooshrooms breed, there is a 1/1024 chance of producing a brown baby. If a red mooshroom breeds with a brown one, there’s a 50% chance of the offspring being red or brown.

Mooshrooms have unique abilities, including the ability to be milked for mushroom stew. When sheared, they drop mushrooms and revert to a regular cow form. 

## Wolf
Wolves are neutral mobs that can be found in forests and taiga biomes. They can be tamed using bones, with the number needed varying randomly. Once tamed, wolves become loyal companions and can be commanded to sit or follow the player.

Tamed wolves have 10 hearts of health, while untamed wolves have 8 hearts. They can enter love mode and breed when fed meat items, producing a pup that trusts the player. If two different players own the parents, the pup will randomly follow one of them.

Wolves can attack hostile mobs, such as skeletons and zombies, and will defend their owners if they are attacked. When they take damage, they may howl, and their tails indicate their health level—lower tails mean less health.

## Cat
Cats can spawn in villages or swamp huts. They can be tamed using raw fish, such as raw salmon or raw cod. Once tamed, cats become loyal companions and can follow players or be commanded to sit, just like wolves. 

Tamed cats have unique behaviors, including the ability to sleep on beds and occasionally bring gifts to their owners, such as feathers or raw fish. They can also scare away creepers.

Cats come in various breeds and color patterns, and their appearance is determined by random generation. When breeding, two tamed cats can produce a kitten, which will inherit some characteristics from its parents.

## Ocelot
To tame an ocelot, players must use raw fish, such as raw salmon or raw cod. Once tamed, ocelots transform into domesticated cats, which can be commanded to sit or follow the player. Ocelots can breed with each other, resulting in kittens that inherit traits from their parents. However, they remain wild and do not become fully tame like domestic cats. Tamed ocelots behave like tamed cats, like the ability to sit on beds and occasionally bring gifts to their owners. 

## Farm animals
### Cow
They are bred using wheat and their offspring, a calf, follows adult cows. They wander aimlessly but tend to stay in well-lit and grassy areas. When hit, they will flee and are 25% faster for a few seconds. 

### Goat
Goats tend to ram every 30 to 300 seconds. Ramming is when a goat speeds towards an unmoving target to deal 1 to 3 hearts of damage. Goats are also able to jump 10 blocks vertically and 5 blocks laterally. Goats are also bred using wheat. When breeding a normal goat with a screaming goat (goat that screams and rams more often), there’s a 50% chance the offspring will be a screaming goat as well. There is a 2% spawn rate chance of a screaming goats as well. 

### Sheep
The majority of sheep are born white, with a spawn rate of approximately 81.836%. Other color variations include black, gray, and light gray (each at 5%), brown (3%), and pink (0.164%). 

When breeding (using wheat), if the parent sheep have compatible wool colors, the offspring will inherit a mix of those colors. For example, breeding a blue sheep with a white sheep can result in a light blue sheep. Sheep are also known for their ability to be sheared for wool, which can be dyed in various colors. This makes them useful for crafting and decoration in the game. 

### Pig
Pigs can be bred using carrots, potatoes, or beetroot. They can be ridden with a saddle using a carrot on a stick to control it. 

### Chicken
Chickens can be bred using seeds, such as wheat seeds, melon seeds, or beetroot seeds to spawn a chick. Players can also throw eggs, which are randomly dropped by chickens, to spawn chicks with a 1/8 chance. If an egg is thrown, there’s a 1/32 chance of spawning three additional baby chickens, resulting in a 1/256 overall chance for a total of four chicks.
