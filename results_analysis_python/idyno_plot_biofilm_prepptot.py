#!/usr/bin/python
from __future__ import division
from __future__ import with_statement
import os
from optparse import OptionParser
import toolbox_idynomics
import toolbox_plotting_age as toolbox_plotting
from mpl_toolkits.axes_grid1 import make_axes_locatable
import toolbox_results
import matplotlib
import numpy
import matplotlib.pyplot as plt

parser = OptionParser()
parser.add_option("-a", "--AllIter", dest="all_iter", default=False,
                  action="store_true", help="plot all iterates, ignoring -i")
parser.add_option("-b", "--ColorBar", dest="color_bar", default=False,
                            action="store_true", help="include a colorbar")
parser.add_option("-d", "--DotsPerInch", dest="dpi", default=300, type="int",
                                              help="output figure resolution")
parser.add_option("-e", "--FileExt", dest="file_ext", default=".png",
                                      help="file extension for figure output")
parser.add_option("-f", "--FrameOn", dest="frameon", default=True,
                        action="store_true", help="turn the figure frame on")
parser.add_option("-F", "--FigureType", dest="figure_type", default=None,
                         help="type of figure to use. Default is 'Slide'")
parser.add_option("-H", "--Height", dest="height", default=0,
                    type="int", help="figure height in inches")
parser.add_option("-i", "--IterateNum", dest="iter_num", default=1441,
                    type="int", help="number of the iterate to be plotted")
parser.add_option("-I", "--IMax", dest="i_max", default=0,
                        type="int", help="maximum height to plot")
parser.add_option("--rr", "--ResultsDirr", dest="results_dirr",
                      default=os.getcwd(), help="path to results directory")
parser.add_option("--Rr", "--ResultsDiRr", dest="results_diRr",
                      default=os.getcwd(), help="path to results directory")
parser.add_option("--RR", "--ResultsDiRR", dest="results_diRR",
                      default=os.getcwd(), help="path to results directory")
parser.add_option("--rrr", "--ResultsDirrr", dest="results_dirrr",
                      default=os.getcwd(), help="path to results directory")
parser.add_option("--rrR", "--ResultsDirrR", dest="results_dirrR",
                      default=os.getcwd(), help="path to results directory")
parser.add_option("-s", "--SoluteName", dest="solute_name", default="glucose",
                        help="name of the solute to be plotted behind cells")
parser.add_option("-S", "--Substratum", dest="substratum", default=True,
                  action="store_false", help="turn off drawing the substratum")
parser.add_option("-t", "--TimeOn", dest="timeon", default=True,
                        action="store_true", help="record the time in figures")
parser.add_option("-T", "--TitleOn", dest="titleon", default=False,
                        action="store_true", help="turn the figure title on")
parser.add_option("--TitleScript", help="script of the title",
                   dest="titlescript", default=None)
parser.add_option("-W", "--Width", dest="width", default=0,
                    type="int", help="figure width in inches")
parser.add_option("-z", "--ZeroColorBar", dest="zero_color", default=False,
                    action="store_true",
                    help="forces the lower limit of the color bar to zero")
parser.add_option("--sp", dest="speciesnumber", default=1, type="int", 
                  help="how many species in the biofilm, script allows for 2")
parser.add_option("--N", dest="numplots", default=1, type="int",
                  help="allows more than one biofilm to be plotted in the same figure")
parser.add_option("--L", dest="lastIter", default=True,
                        action="store_false", help="take the last iteration of the simulation")
parser.add_option("--TP", dest="timepoints", default=1, type="int",
                  help="takes more than one timepoint")
(options, args) = parser.parse_args()

numplots = options.numplots
sp = options.speciesnumber
TP = options.timepoints
if numplots == 1:
    sim1 = toolbox_idynomics.SimulationDirectory(options.results_dirr)
if numplots == 2:
    sim1 = toolbox_idynomics.SimulationDirectory(options.results_dirr)
    sim2 = toolbox_idynomics.SimulationDirectory(options.results_diRr)
if numplots == 3:
    sim1 = toolbox_idynomics.SimulationDirectory(options.results_dirr)
    sim2 = toolbox_idynomics.SimulationDirectory(options.results_diRr)
    sim3 = toolbox_idynomics.SimulationDirectory(options.results_diRR)
if numplots == 4:
    sim1 = toolbox_idynomics.SimulationDirectory(options.results_dirr)
    sim2 = toolbox_idynomics.SimulationDirectory(options.results_diRr)
    sim3 = toolbox_idynomics.SimulationDirectory(options.results_diRR)
    sim4 = toolbox_idynomics.SimulationDirectory(options.results_dirrr)
if numplots == 5:
    sim1 = toolbox_idynomics.SimulationDirectory(options.results_dirr)
    sim2 = toolbox_idynomics.SimulationDirectory(options.results_diRr)
    sim3 = toolbox_idynomics.SimulationDirectory(options.results_diRR)
    sim4 = toolbox_idynomics.SimulationDirectory(options.results_dirrr)
    sim5 = toolbox_idynomics.SimulationDirectory(options.results_dirrR)



if not options.file_ext[0] == '.':
    options.file_ext = '.'+options.file_ext

save_name = 'biofilm_'+options.solute_name

num_digits = len(str(sim1.get_last_iterate_number()))

nI, nJ, nK, res = sim1.find_domain_dimensions()
if options.i_max > 0:
    nI = options.i_max

counter = 0

if options.figure_type == None:
    if options.height > 0: height = options.height
    else: height = toolbox_plotting.mm2inch(nI * res * numplots)
    if options.width > 0: width = (options.width*TP)
    else: width = toolbox_plotting.mm2inch(nJ * res * TP)
    figure = toolbox_plotting.SlideFigure(width=width, height=height)
else:
    script = "figure = toolbox_plotting."+options.figure_type+"Figure("
    if nI > 2*nJ:
        script += "height='double'"
    elif nJ > 2*nI:
        script += "double_column=True, height='single'"
    else:
        script += "double_column=True, height='double'"
    script += ")"
    try:
        exec(script)
    except:
        print 'Could not make figure!'
        print script      
        
species_color_dict = {'OldieA' : 'cool', 'OldieB' : 'Greens', 'OldieC' : 'hot'}
def color_cells_by_age_and_species(agent_output, species_color_dict):   
    for species in agent_output.species_outputs:
        if species.members == []:
            continue
        print('Colouring %d %s cells %s'%(len(species.members),
                            species.name, species_color_dict[species.name]))
        for cell in species.members:
            name = species.name
            for (species_name, colormap_name) in species_color_dict.iteritems():
                if name == species_name:
                    norm = matplotlib.colors.Normalize(vmin=0, vmax=1)
                    colormap = matplotlib.cm.get_cmap(colormap_name, 256)
                    m = matplotlib.cm.ScalarMappable(norm=norm, cmap=colormap)
                    age = cell.vars['age']
                    age = float(age)
                cell.color = m.to_rgba(age)
                
species_color_dict = {'OldieA' : 'hsv', 'OldieB' : 'hot', 'OldieC' : 'cool'}
def color_cells_by_growth_and_species(agent_output, species_color_dict):
    biomass_names=['activeBiomassGrowth', 'activeBiomassRepair',
                                       'inactiveBiomassGrowth', 'inactiveBiomassRepair']
    for species in agent_output.species_outputs:
        if species.members == []:
            continue
        print('Colouring %d %s cells %s'%(len(species.members),
                            species.name, species_color_dict[species.name]))
        for cell in species.members:
            name = species.name
            for (species_name, colormap_name) in species_color_dict.iteritems():
                if name == species_name:
                    norm = matplotlib.colors.Normalize(vmin=0, vmax=1)
                    colormap = matplotlib.cm.get_cmap(colormap_name, 256)
                    m = matplotlib.cm.ScalarMappable(norm=norm, cmap=colormap)
                    growthrate = cell.get_specific_growth_rate(biomass_names)
                    growthrate = float(growthrate)
                cell.color = m.to_rgba(growthrate)

def color_cells_by_Prep_Ptot_and_species(agent_output, species_color_dict):

    for species in agent_output.species_outputs:
        if species.members == []:
            continue
        print('Colouring %d %s cells %s'%(len(species.members),
                            species.name, species_color_dict[species.name]))
        for cell in species.members:
            name = species.name
            for (species_name, colormap_name) in species_color_dict.iteritems():
                if name == species_name:
                    norm = matplotlib.colors.Normalize(vmin=0, vmax=1)
                    colormap = matplotlib.cm.get_cmap(colormap_name, 256)
                    m = matplotlib.cm.ScalarMappable(norm=norm, cmap=colormap)
                    pag = cell.vars['activeBiomassGrowth']
                    pag = float(pag)
                    par = cell.vars['activeBiomassRepair']
                    par = float(par)
                    pdg = cell.vars['inactiveBiomassGrowth']
                    pdg = float(pdg)
                    pdr = cell.vars['inactiveBiomassRepair']
                    pdr = float(pdr)
                    prep = par+pdr
                    ptot = pag+par+pdg+pdr
                    prepptot = float(prep/ptot)
                cell.color = m.to_rgba(prepptot)
                
def colorbar(axis, position="bottom", s=1, label='Age', pad=0.55):
    cmap = matplotlib.cm.get_cmap('cool', 256)
    a = numpy.array([[0,1]])
    norm = matplotlib.colors.Normalize(vmin=0, vmax=1)
    m = matplotlib.cm.ScalarMappable(norm=norm, cmap=cmap)
    m.set_array(a)
    cbar = toolbox_plotting.make_colorbar(axis, m, side=position, pad=pad, label=label)
    return cbar


agegrid = [0]*nJ
locationX = [0]*nJ
def get_agegrid(i_info):
    print('Getting cell ages')
    for r in i_info.agent_output.get_all_cells():
        x = float(r.vars['locationX'])
        x = int(x)
        locationX[x] = x
        age = float(r.vars['age'])
        if age == 0:
            continue
        else:
            agegrid[x] += age
            agegrid[x] /= 2
    maximum = max(locationX)+1
    agegrid2 = [0]*maximum
    locationX2 = [0]*maximum
    for a in range(maximum):
        agegrid2[a] = agegrid[a]
        locationX2[a] = locationX[a]
    return agegrid2, locationX2

 
def get_axes(numplots, plot, tp):
    col = TP*10  
    row = numplots*100
    tp += 1
    plot += 1
    num = plot*TP-(TP-tp)
    axistouse = figure.add_subplot('', row+col+num, frameon=options.frameon)
    return axistouse
            
def plot(sim, plot):
    if options.lastIter:
        iter_info = sim.get_last_iterate_number()        
    else:
        iter_info = options.iter_num
    alliters = sim.get_iterate_numbers()
    best = iter_info
    for i in range(TP):
        minimum = 20
        if not TP == 1:
            listiter = ((i+1)*iter_info)/TP
            for j in range(len(alliters)):
                if abs(alliters[j]-listiter) < minimum:
                    minimum = abs(alliters[j]-listiter)
                    best = alliters[j]
        i_info = sim.get_single_iterate(best)
        axis = get_axes(numplots, plot, i)
        color_cells_by_Prep_Ptot_and_species(i_info.agent_output, species_color_dict)
        min_max_concns = i_info.get_min_max_concns()
        toolbox_idynomics.plot_cells_2d(axis, i_info.agent_output)

        if numplots == 1 and sp == 1:
            cbar = colorbar(axis)
    
        if sp == 1:
            ages, locationX = get_agegrid(i_info)
            divider = make_axes_locatable(axis)
            cax = divider.append_axes("right", size="30%", pad=0.2)
            cax.plot(ages, locationX)
            cax.set_xlim(0,1)
            cax.set_xticks([0, 0.5, 1.0])
            cax.set_yticks([])
            cax.set_xlabel('Mean Age')
            cax.set_ylim(0, nI * res)
        
        
        if options.substratum:
            axis.fill_between([0, nJ*res], [0]*2, y2=[-res]*2, color='k', zorder=-1)
        lb = 0.01
    
    
        if not options.solute_name == "none":
            #print min_max_concns[options.solute_name]
            solute_output = toolbox_results.SoluteOutput(i_info.env_output,
                                                         name=options.solute_name)
            cs = toolbox_idynomics.solute_contour(axis, solute_output,
                                concn_range=min_max_concns[options.solute_name],
                                                        interpolation='bicubic')
        if options.titleon and options.titlescript is None:
            axis.set_title(r'Biofilm (%s g L$^{-1}$)'%(options.solute_name))
        if not options.titlescript is None:
            axis.set_title(options.titlescript)
        if options.frameon:
            #axis.set_xlabel(r'x ($\mu$m)')
            axis.set_ylabel(r'y ($\mu$m)', fontsize=12)
        if options.timeon:
            axis.text(0.1*res, ((nI+0.1)*res-8), 'Time: %d h'%(int(i_info.time)),
                      va='bottom', ha='left', color='#bd0303', fontsize=11)
        xmax = nJ * res
        axis.set_xlim(0, xmax)
        axis.set_ylim(0, nI * res)
        axis.set_xlabel(r'x ($\mu$m)', fontsize=12)
        #axis.set_ylim(0, 50)
        if numplots > 1 and plot == 0:
            axis.set_title(r'$\beta $= 0.06', fontsize=16, loc='left')
            if not options.titlescript is None:
                axis.set_title(options.titlescript, fontsize = 20, loc='center', x= -1)
        if plot == 1:
            axis.set_title(r'$\beta $= 0.07', fontsize=16, loc='left')
            axis.set_title('', fontsize = 16, loc='center')
        if plot == 2:
            axis.set_title(r'$\beta $= 0.08', fontsize=16, loc='left')
            axis.set_title('', fontsize = 16, loc='center')
        if plot == 3:
            axis.set_title('No repair', fontsize=16, loc='left')
            axis.set_title('', fontsize = 16, loc='center')
        if plot == 4:
            axis.set_title('Adaptive repair', fontsize=16, loc='left')
            axis.set_title('', fontsize = 16, loc='center')
        
        wspace = 0.1
        hspace = 0.4
        if not nI == 65:
            hspace=0.3
        if plot == numplots-1 and i == TP-1:
            figure.subplots_adjust(left=lb, bottom=lb, right=0.9, top=0.9, wspace=wspace, hspace=hspace)
            figure.process_subplots()
            figure.inset_axes()
            save_num = str(i_info.number)
            save_num = (num_digits - len(save_num))*'0' + save_num
            save_path = os.path.join(sim.figures_dir, save_name+'_'+save_num+'_prep_ptot'+options.file_ext)
            figure.save(save_path, dpi=options.dpi)


for j in range(numplots):
    if j == 0:
        sim = sim1
    elif j == 1:
        sim = sim2
    elif j == 2:
        sim = sim3
    elif j == 3:
        sim = sim4
    elif j == 4:
        sim = sim5
    else:
        sim = sim1
    plot(sim, j)