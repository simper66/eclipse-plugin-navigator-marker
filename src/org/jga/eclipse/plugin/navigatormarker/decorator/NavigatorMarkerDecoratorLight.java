package org.jga.eclipse.plugin.navigatormarker.decorator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaModelMarker;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.ui.IDecoratorManager;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.ui.progress.UIJob;

import org.jga.eclipse.plugin.navigatormarker.plugin.NavigatorMarkerPlugin;

public class NavigatorMarkerDecoratorLight extends LabelProvider implements ILightweightLabelDecorator, IResourceChangeListener {
	
	private final static String ID = "org.jga.navmark.decoratorLight";
	
	private final static String UIJob_NAME = "ProblemRedecoration";
	
	private final static QualifiedName NEW_DECORATION_QN = new QualifiedName(NavigatorMarkerDecoratorLight.ID, "newDecoration");

	private final static ImageDescriptor IMG_ERROR;
	private final static ImageDescriptor IMG_WARNING;
	private final static ImageDescriptor IMG_EXCLAMATION;
	static {
		IMG_ERROR = AbstractUIPlugin.imageDescriptorFromPlugin(NavigatorMarkerPlugin.ID, "icons/myerror.gif");
		IMG_WARNING = AbstractUIPlugin.imageDescriptorFromPlugin(NavigatorMarkerPlugin.ID, "icons/mywarning.gif");
		IMG_EXCLAMATION = AbstractUIPlugin.imageDescriptorFromPlugin(NavigatorMarkerPlugin.ID, "icons/myexclamation.gif");
	}
	
	public NavigatorMarkerDecoratorLight() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}
	
	public IBaseLabelProvider getDecorator() {
		IDecoratorManager decoratorManager = NavigatorMarkerPlugin.getDefault().getWorkbench().getDecoratorManager();
		return decoratorManager.getBaseLabelProvider(NavigatorMarkerDecoratorLight.ID);
	}
	
	public boolean isDecoratorEnabled() {
		IDecoratorManager decoratorManager = NavigatorMarkerPlugin.getDefault().getWorkbench().getDecoratorManager();
		return decoratorManager.getEnabled(NavigatorMarkerDecoratorLight.ID);
	}
	
	private IResource getResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource)((IAdaptable)object).getAdapter(IResource.class);
		}
		return null;
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {

		try {

			if (!this.isDecoratorEnabled()) return;

			IResource rsc = getResource(element);
			Integer severity = (Integer)rsc.getSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN);
			if (severity==null) {
				severity = rsc.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (severity==0) severity=-1;
				else if (rsc instanceof IProject) {
					IMarker[] markers = ((IResource)rsc).findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
					if (markers.length>0) {
						for (int markerNum=0 ; markerNum<markers.length ; markerNum++) {
							if (IJavaModelMarker.BUILDPATH_PROBLEM_MARKER.equals(markers[markerNum].getType())) {
								severity=0;
								break;
							}
						}
					}
				}
			}
			
			if (severity<IMarker.SEVERITY_INFO) return;

			if (severity==IMarker.SEVERITY_ERROR) {
				decoration.addOverlay(NavigatorMarkerDecoratorLight.IMG_ERROR, IDecoration.BOTTOM_LEFT);
			} else if (severity==IMarker.SEVERITY_WARNING) {
				decoration.addOverlay(NavigatorMarkerDecoratorLight.IMG_WARNING, IDecoration.BOTTOM_LEFT);
			} else if (severity==IMarker.SEVERITY_INFO) {
				decoration.addOverlay(NavigatorMarkerDecoratorLight.IMG_EXCLAMATION, IDecoration.BOTTOM_LEFT);
			}

		} catch (Throwable t) {
			NavigatorMarkerPlugin.logError(NavigatorMarkerDecoratorLight.ID, t);
		}

		return;
		
	}
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		
		if (!this.isDecoratorEnabled()) return;

		if (event.getType()==IResourceChangeEvent.POST_BUILD || event.getType()==IResourceChangeEvent.POST_CHANGE ) {
			try{ 

				List<IResource> resourcesToRedecorate = new ArrayList<IResource>();
				IMarkerDelta[] deltaMarkers = event.findMarkerDeltas(IMarker.PROBLEM, true);

				//FOR EACH DELTA MARKER
				for (IMarkerDelta deltaMarker: deltaMarkers) {
					IResource rsc = this.getResource(deltaMarker.getResource());
					if (!rsc.isAccessible()) continue;
					
					if (rsc instanceof IProject) { //IF ITS RESOURCE IS A IPROJECT WE SEE WHETHER IT EXISTS BUILD PATH PROBLEMS
						
						IMarker[] markers = ((IResource)rsc).findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
						if (markers.length>0) {
							for (int markerNum=0 ; markerNum<markers.length ; markerNum++) {
								if (IJavaModelMarker.BUILDPATH_PROBLEM_MARKER.equals(markers[markerNum].getType())) {
									int rscIdx = resourcesToRedecorate.indexOf(rsc);
									if (rscIdx==-1) {//FIRST MIGHT BE "CLEAR"
										rsc.setSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN, -1);
										resourcesToRedecorate.add(rsc);
									} else {//IF A SECOND ONE CAMES IT MIGHT BE "PUT ERROR" BEACAUSE IT HAS NOT BEEN FIXED
										rsc.setSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN, IMarker.SEVERITY_INFO);
										resourcesToRedecorate.set(rscIdx, rsc);
									}
								}
							}
						}
					
					} else if (rsc instanceof IFile && resourcesToRedecorate.indexOf(rsc)==-1) {//IF ITS RESOURCE IS A IFILE AND IT HAVE NOT BEEN DEALED BY ANOTHER DELTAMARKER

						//ITS WORST DECORATOR. -1 AND 0 ARE ASIGNED TO -1 (CLEAR). 0 IS USED FOR THE EXCLAMACION
						int severity=rsc.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
						if (severity==0) severity=-1;

						//WE INSERT A NEW DECORATOR IN ITS SESSION AND IS ADDED TO THE LIST
						rsc.setSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN, severity);
						resourcesToRedecorate.add(rsc);

						//DEALING WITH ITS PARENTS
						while ((rsc=rsc.getParent()).getParent()!=null) {

							//IF THE PARENT DOES NOT EXISTS IT IS ADDED WITH ITS CHILD DECORATOR. 
							int rscIdx = resourcesToRedecorate.indexOf(rsc);
							if (rscIdx==-1) {
								rsc.setSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN, severity);
								resourcesToRedecorate.add(rsc);

							//IF THE PARENT EXISTS AND THIS CHILD HAS ERROR O WARNING DECORATOR
							} else if (severity>IMarker.SEVERITY_INFO) {
								//WE TAKE THE WORST DECORATOR OF THIS PARENT UNTIL NOW
								int worstSeverity = (int)resourcesToRedecorate.get(rscIdx).getSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN);
								//IF THIS CHILD DECORATOR IS WORST WE REPLACE 
								if (severity>worstSeverity) {
									rsc.setSessionProperty(NavigatorMarkerDecoratorLight.NEW_DECORATION_QN, severity);
									resourcesToRedecorate.set(rscIdx, rsc);
								}
							}

						}//WHILE PARENT

					}//RESOURCE NOT DEALED

				}//DELTA MARKERS

				//FIRE THE DECORATION OF THE RESOURCES
				if (resourcesToRedecorate.size()>0) this.redecorate(resourcesToRedecorate);

			} catch (Throwable t) {
				NavigatorMarkerPlugin.logError(NavigatorMarkerDecoratorLight.ID, t);
			}
		}

	}
	
	public void redecorate(List<IResource> resourcesToBeUpdated) {
		if (!this.isDecoratorEnabled()) return;
		final LabelProviderChangedEvent labelProvider = new LabelProviderChangedEvent(this.getDecorator(), resourcesToBeUpdated.toArray());
		new UIJob(NavigatorMarkerDecoratorLight.UIJob_NAME) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				fireLabelProviderChanged(labelProvider);
				return Status.OK_STATUS;
			}
		}.schedule();
	}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	
}
