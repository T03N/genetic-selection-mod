import numpy as np
import matplotlib.pyplot as plt

def generate_breeding_damage(initial_damage, breeding_bonus, max_cap, generations):
    """
    Generate damage values over generations with exponential growth and cap.

    Parameters:
    -----------
    initial_damage : float
        Starting base attack damage (D₀)
    breeding_bonus : float
        Percentage increase per generation (e.g., 0.05 for 5%)
    max_cap : float
        Maximum damage cap
    generations : int
        Number of generations to simulate

    Returns:
    --------
    gen_numbers : array
        Array of generation numbers [0, 1, 2, ..., generations]
    damage_values : array
        Array of damage values for each generation
    """
    gen_numbers = np.arange(0, generations + 1)
    damage_values = np.zeros(generations + 1)

    # Calculate damage for each generation
    for n in gen_numbers:
        damage = initial_damage * ((1 + breeding_bonus) ** n)
        damage_values[n] = min(damage, max_cap)

    return gen_numbers, damage_values

def plot_breeding_damage(initial_damage, breeding_bonus, max_cap, generations):
    """
    Plot the damage progression over generations.
    """
    gen_numbers, damage_values = generate_breeding_damage(
        initial_damage, breeding_bonus, max_cap, generations
    )

    plt.figure(figsize=(10, 6))
    plt.plot(gen_numbers, damage_values, 'b-', linewidth=2, label='Damage')
    plt.axhline(y=max_cap, color='r', linestyle='--', linewidth=1.5,
                label=f'Max Cap ({max_cap})')
    plt.xlabel('Generation', fontsize=12)
    plt.ylabel('Attack Damage', fontsize=12)
    plt.title('Breeding Damage Progression Over Generations', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.show()

    return gen_numbers, damage_values

# Example usage:
initial_damage = 5.0
breeding_bonus = 0.05  # 5% increase per generation
max_cap = 20.0
generations = 50

gen, damage = plot_breeding_damage(initial_damage, breeding_bonus, max_cap, generations)

# Print some statistics
print(f"Initial damage: {damage[0]:.2f}")
print(f"Damage at generation 10: {damage[10]:.2f}")
print(f"Damage at generation 20: {damage[20]:.2f}")
print(f"Damage at generation 30: {damage[30]:.2f}")

# Find when cap is reached
cap_reached = np.where(damage >= max_cap)[0]
if len(cap_reached) > 0:
    print(f"\nCap reached at generation: {cap_reached[0]}")
else:
    print(f"\nCap not reached within {generations} generations")